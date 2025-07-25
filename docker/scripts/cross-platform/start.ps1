# 크로스 플랫폼 진입점 스크립트 (Windows PowerShell)

Write-Host "🐳 TechWikiPlus Docker Manager" -ForegroundColor Green
Write-Host "OS detected: Windows (PowerShell)" -ForegroundColor Blue
Write-Host ""

# 스크립트 디렉토리 경로
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# PowerShell 실행 정책 확인
$executionPolicy = Get-ExecutionPolicy
if ($executionPolicy -eq "Restricted") {
    Write-Host "⚠️  PowerShell 실행 정책이 제한되어 있습니다." -ForegroundColor Yellow
    Write-Host "다음 명령을 관리자 권한으로 실행해주세요:" -ForegroundColor Yellow
    Write-Host "  Set-ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "종료하려면 Enter를 누르세요..."
    exit 1
}

# Windows Docker Manager 실행
$dockerManagerPath = Join-Path $scriptDir "..\windows\docker-manager.ps1"

if (Test-Path $dockerManagerPath) {
    & $dockerManagerPath
}
else {
    Write-Host "Error: docker-manager.ps1 not found!" -ForegroundColor Red
    Write-Host "Please ensure the script exists at: $dockerManagerPath" -ForegroundColor Red
    Read-Host "종료하려면 Enter를 누르세요..."
    exit 1
}