param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$version = '1.0.0'

$services = @(
    @{ Name = 'user-service';        Port = 8081 },
    @{ Name = 'content-service';     Port = 8082 },
    @{ Name = 'interaction-service'; Port = 8085 },
    @{ Name = 'recommend-service';   Port = 8083 },
    @{ Name = 'ai-service';          Port = 8084 },
    @{ Name = 'gateway-service';     Port = 8080 }
)

$logDir = Join-Path $root 'logs'
$dataDir = Join-Path $root 'data'
New-Item -ItemType Directory -Force -Path $logDir, $dataDir | Out-Null
$env:DB_DIR = $dataDir

# 读取本地未跟踪的环境变量文件（.env.local），用于注入 DeepSeek Key 等敏感配置
$envFile = Join-Path $root '.env.local'
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*#' -or -not $_.Trim()) { return }
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()
            if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
            }
        }
    }
}

function Test-PortOpen([int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect('127.0.0.1', $port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

if (-not $SkipBuild) {
    $missing = @($services | Where-Object {
            -not (Test-Path (Join-Path $root "$($_.Name)\target\$($_.Name)-$version.jar"))
        })
    if ($missing.Count -gt 0) {
        Write-Host '==> 正在构建后端（首次构建需要下载依赖，请耐心等待）...' -ForegroundColor Cyan
        Push-Location $root
        try {
            & mvn.cmd -B -DskipTests package
            if ($LASTEXITCODE -ne 0) {
                throw '后端构建失败，请查看上方 Maven 输出'
            }
        } finally {
            Pop-Location
        }
    }
}

Write-Host '==> 启动微服务...' -ForegroundColor Cyan
$started = @()

foreach ($svc in $services) {
    if (Test-PortOpen $svc.Port) {
        Write-Host ("  - {0,-22} 端口 {1} 已被占用，跳过" -f $svc.Name, $svc.Port) -ForegroundColor Yellow
        continue
    }

    $jar = Join-Path $root "$($svc.Name)\target\$($svc.Name)-$version.jar"
    if (-not (Test-Path $jar)) {
        throw "缺少可执行包：$jar"
    }

    $stdout = Join-Path $logDir "$($svc.Name).log"
    $stderr = Join-Path $logDir "$($svc.Name).err.log"
    $process = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) `
        -WorkingDirectory $root -RedirectStandardOutput $stdout -RedirectStandardError $stderr `
        -WindowStyle Hidden -PassThru

    $process.Id | Set-Content -Path (Join-Path $logDir "$($svc.Name).pid") -Encoding ascii
    $started += [pscustomobject]@{ Name = $svc.Name; Port = $svc.Port; Process = $process }
    Write-Host ("  - {0,-22} 启动中 (PID {1})" -f $svc.Name, $process.Id) -ForegroundColor Green
}

if ($started.Count -eq 0) {
    Write-Host '所有服务都已在运行。' -ForegroundColor Yellow
} else {
    Write-Host '==> 等待服务就绪...' -ForegroundColor Cyan
    foreach ($svc in $started) {
        $url = "http://127.0.0.1:$($svc.Port)/actuator/health"
        $deadline = (Get-Date).AddSeconds(150)
        $ready = $false
        while ((Get-Date) -lt $deadline) {
            if ($svc.Process.HasExited) {
                break
            }
            try {
                $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 2
                if ($response.StatusCode -eq 200) {
                    $ready = $true
                    break
                }
            } catch {
                Start-Sleep -Milliseconds 1500
            }
        }
        if ($ready) {
            Write-Host ("  ✔ {0,-22} http://localhost:{1}" -f $svc.Name, $svc.Port) -ForegroundColor Green
        } else {
            Write-Host ("  ✘ {0,-22} 启动失败，日志：logs\{0}.log" -f $svc.Name) -ForegroundColor Red
        }
    }
}

Write-Host ''
Write-Host '后端已就绪：' -ForegroundColor Cyan
Write-Host '  网关      http://localhost:8080'
Write-Host '  前端工程   独立仓库 lay-coo-frontend，启动后访问 http://localhost:5173'
Write-Host ''
Write-Host '演示账号：admin / admin123（管理员）、demo / demo123（普通用户）'
Write-Host '停止服务：scripts\stop-backend.cmd'
