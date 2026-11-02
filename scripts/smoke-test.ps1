$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'
$script:passed = 0
$script:failed = 0

function Invoke-Api {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = ''
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $request = @{
        Uri        = "$base$Path"
        Method     = $Method
        Headers    = $headers
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $request['ContentType'] = 'application/json; charset=utf-8'
        $request['Body'] = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    $response = Invoke-RestMethod @request
    if ($response.code -ne 0) {
        throw "接口返回业务错误（$Path）：$($response.msg)"
    }
    return $response.data
}

function Check {
    param([string]$Name, [bool]$Condition, [string]$Detail = '')
    if ($Condition) {
        Write-Host ("  [PASS] {0}" -f $Name) -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host ("  [FAIL] {0} {1}" -f $Name, $Detail) -ForegroundColor Red
        $script:failed++
    }
}

Write-Host '=== 岚途 LayCoo 冒烟测试 ===' -ForegroundColor Cyan

try {
    $health = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 10
    Check '网关健康检查' ($health.status -eq 'UP') $health.status
} catch {
    Check '网关健康检查' $false $_.Exception.Message
}

try {
    $categories = Invoke-Api -Path '/category/list'
    Check '分类列表' ($categories.Count -ge 9) "count=$($categories.Count)"
} catch {
    Check '分类列表' $false $_.Exception.Message
}

$contents = $null
try {
    $contents = Invoke-Api -Path '/content/list?page=1&size=6&sort=hot'
    Check '内容列表' ($contents.total -gt 0 -and $contents.list.Count -gt 0) "total=$($contents.total)"
} catch {
    Check '内容列表' $false $_.Exception.Message
}

$firstId = $null
if ($contents -and $contents.list.Count -gt 0) {
    $firstId = $contents.list[0].id

    try {
        $detail = Invoke-Api -Path "/content/$firstId"
        Check '内容详情' ($detail.id -eq $firstId -and $detail.body.Length -gt 50) "title=$($detail.title)"
    } catch {
        Check '内容详情' $false $_.Exception.Message
    }

    try {
        $related = Invoke-Api -Path "/content/$firstId/related?size=4"
        Check '相关推荐' (@($related).Count -gt 0) "count=$(@($related).Count)"
    } catch {
        Check '相关推荐' $false $_.Exception.Message
    }
} else {
    Check '内容详情' $false '前置条件：内容列表为空'
}

try {
    $search = Invoke-Api -Path '/search?keyword=%E8%A5%BF%E6%B9%96&page=1&size=9'
    Check '关键词搜索' ($search.total -gt 0) "total=$($search.total)"
} catch {
    Check '关键词搜索' $false $_.Exception.Message
}

try {
    $homeRecs = Invoke-Api -Path '/recommend/home?size=8'
    Check '首页推荐' ($homeRecs.Count -gt 0 -and $homeRecs[0].reason) "count=$($homeRecs.Count)"
} catch {
    Check '首页推荐' $false $_.Exception.Message
}

try {
    $hotRecs = Invoke-Api -Path '/recommend/hot?limit=8'
    Check '热门榜单' ($hotRecs.Count -gt 0) "count=$($hotRecs.Count)"
} catch {
    Check '热门榜单' $false $_.Exception.Message
}

$userToken = $null
try {
    $auth = Invoke-Api -Method 'POST' -Path '/user/login' -Body @{ username = 'demo'; password = 'demo123' }
    $userToken = $auth.token
    Check '用户登录' ([bool]$auth.token -and $auth.user.username -eq 'demo')
} catch {
    Check '用户登录' $false $_.Exception.Message
}

if ($userToken -and $firstId) {
    try {
        $info = Invoke-Api -Path '/user/info' -Token $userToken
        Check '获取用户信息' ($info.username -eq 'demo')
    } catch {
        Check '获取用户信息' $false $_.Exception.Message
    }

    try {
        Invoke-Api -Method 'POST' -Path '/favorite' -Body @{ contentId = $firstId } -Token $userToken | Out-Null
        $state = Invoke-Api -Path "/favorite/status?contentId=$firstId" -Token $userToken
        Check '收藏内容' ($state.favorited -eq $true)

        $favList = Invoke-Api -Path '/favorite/list?page=1&size=10' -Token $userToken
        Check '收藏列表' ($favList.total -ge 1)
    } catch {
        Check '收藏内容' $false $_.Exception.Message
    }

    try {
        $like = Invoke-Api -Method 'POST' -Path '/like' -Body @{ contentId = $firstId } -Token $userToken
        Check '点赞' ($like.liked -eq $true)
    } catch {
        Check '点赞' $false $_.Exception.Message
    }

    $commentId = $null
    try {
        $comment = Invoke-Api -Method 'POST' -Path '/comment' -Body @{ contentId = $firstId; content = '接口冒烟测试评论：整体体验不错。' } -Token $userToken
        $commentId = $comment.id
        Check '发表评论' ([bool]$comment.id)

        $comments = Invoke-Api -Path "/interaction/comment/list?contentId=$firstId&page=1&size=10"
        Check '评论列表' ($comments.total -ge 1)
    } catch {
        Check '发表评论' $false $_.Exception.Message
    }

    if ($commentId) {
        try {
            Invoke-Api -Method 'DELETE' -Path "/comment/$commentId" -Token $userToken | Out-Null
            Check '删除评论' $true
        } catch {
            Check '删除评论' $false $_.Exception.Message
        }
    }

    try {
        $rate = Invoke-Api -Method 'POST' -Path '/rate' -Body @{ contentId = $firstId; score = 5 } -Token $userToken
        Check '评分' ($rate.count -ge 1 -and [double]$rate.avg -gt 0) "avg=$($rate.avg)"
    } catch {
        Check '评分' $false $_.Exception.Message
    }

    try {
        $stats = Invoke-Api -Path "/interaction/stats?contentId=$firstId" -Token $userToken
        Check '互动统计' ($stats.favorited -eq $true -and $stats.liked -eq $true -and $stats.myRating -eq 5)
    } catch {
        Check '互动统计' $false $_.Exception.Message
    }
}

$adminToken = $null
try {
    $auth = Invoke-Api -Method 'POST' -Path '/user/login' -Body @{ username = 'admin'; password = 'admin123' }
    $adminToken = $auth.token
    Check '管理员登录' ($auth.user.role -eq 'ADMIN')
} catch {
    Check '管理员登录' $false $_.Exception.Message
}

if ($adminToken) {
    try {
        $stats = Invoke-Api -Path '/admin/content/stats' -Token $adminToken
        Check '管理端统计' ($stats.total -gt 0) "total=$($stats.total)"
    } catch {
        Check '管理端统计' $false $_.Exception.Message
    }

    try {
        $pending = Invoke-Api -Path '/admin/content/list?status=PENDING&page=1&size=10' -Token $adminToken
        Check '待审核列表' ($pending.total -ge 1) "total=$($pending.total)"
    } catch {
        Check '待审核列表' $false $_.Exception.Message
    }

    $taskId = $null
    try {
        $task = Invoke-Api -Method 'POST' -Path '/admin/ai/task' -Token $adminToken -Body @{
            categoryCode = 'FOOD'
            area         = '西湖区'
            count        = 1
            keywords     = '茶文化、本地小吃'
        }
        $taskId = $task.id
        Check '创建 AI 任务' ([bool]$task.id)
    } catch {
        Check '创建 AI 任务' $false $_.Exception.Message
    }

    if ($taskId) {
        try {
            $task = $null
            for ($i = 0; $i -lt 40; $i++) {
                Start-Sleep -Seconds 1
                $task = Invoke-Api -Path "/admin/ai/task/$taskId" -Token $adminToken
                if ($task.status -eq 'SUCCESS' -or $task.status -eq 'FAILED') { break }
            }
            Check 'AI 任务完成' ($task.status -eq 'SUCCESS') "status=$($task.status) error=$($task.error)"

            $result = $task.result | ConvertFrom-Json
            Check 'AI 内容入库' ($result.created -ge 1) "created=$($result.created)"

            if ($result.contentIds.Count -gt 0) {
                $newId = $result.contentIds[0]
                $pendingDetail = Invoke-Api -Path "/admin/content/$newId" -Token $adminToken
                Check 'AI 内容进入待审' ($pendingDetail.status -eq 'PENDING') "status=$($pendingDetail.status)"

                Invoke-Api -Method 'POST' -Path '/admin/content/audit' -Token $adminToken -Body @{
                    id     = $newId
                    action = 'APPROVE'
                    remark = '冒烟测试通过'
                } | Out-Null
                $published = Invoke-Api -Path "/content/$newId"
                Check '审核通过后可访问' ($published.status -eq 'PUBLISHED') "status=$($published.status)"
            }
        } catch {
            Check 'AI 生成流程' $false $_.Exception.Message
        }
    }
}

Write-Host ''
Write-Host ("结果：{0} 项通过，{1} 项失败" -f $script:passed, $script:failed) -ForegroundColor Cyan
if ($script:failed -gt 0) {
    exit 1
}
