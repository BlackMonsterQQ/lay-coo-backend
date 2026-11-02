$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root 'logs'

$names = @('gateway-service', 'user-service', 'content-service', 'interaction-service', 'recommend-service', 'ai-service')

foreach ($name in $names) {
    $pidFile = Join-Path $logDir "$name.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host ("  - {0,-22} 未找到 PID 文件" -f $name) -ForegroundColor DarkGray
        continue
    }

    $processId = (Get-Content $pidFile -Raw).Trim()
    if (-not $processId) {
        continue
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host ("  - {0,-22} 进程已退出" -f $name) -ForegroundColor DarkGray
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
        continue
    }

    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    Write-Host ("  ✔ {0,-22} 已停止" -f $name) -ForegroundColor Green
}

Write-Host '完成。' -ForegroundColor Cyan
