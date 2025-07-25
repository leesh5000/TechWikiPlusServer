# 프로덕션 환경을 위한 Docker 실행 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🚀 TechWikiPlus 프로덕션 환경 시작" -ForegroundColor Green

# 디렉토리 설정
Setup-Directories

# .env.prod 파일 확인
if (-not (Test-Path $ENV_PROD)) {
    Write-Host "❌ $ENV_PROD 파일이 없습니다!" -ForegroundColor Red
    Write-Host "$ENV_PROD_EXAMPLE을 복사하고 프로덕션 값을 설정하세요:" -ForegroundColor Yellow
    Write-Host "  cp $ENV_PROD_EXAMPLE $ENV_PROD"
    exit 1
}

# 필수 환경 변수 확인
$requiredVars = @(
    "USER_SERVICE_IMAGE",
    "JWT_SECRET",
    "MYSQL_PASSWORD",
    "REDIS_PASSWORD",
    "MAIL_USERNAME",
    "MAIL_PASSWORD"
)

$missingVars = @()
$envContent = Get-Content $ENV_PROD
foreach ($var in $requiredVars) {
    if (-not ($envContent | Select-String -Pattern "^$var=(?!<)")) {
        $missingVars += $var
    }
}

if ($missingVars.Count -gt 0) {
    Write-Host "❌ 필수 환경 변수가 설정되지 않았습니다:" -ForegroundColor Red
    $missingVars | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

# Docker Compose 명령어 실행
Write-Host "🔄 서비스 시작 중..." -ForegroundColor Green

$cmd = "docker-compose -p `"$PROJECT_NAME_USER_SERVICE`" -f `"$COMPOSE_BASE`" -f `"$COMPOSE_PROD`" --env-file `"$ENV_PROD`" up -d"
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 프로덕션 서비스가 시작되었습니다!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  프로덕션 환경 주의사항:" -ForegroundColor Yellow
    Write-Host "  - SSL/TLS 인증서 설정 필요"
    Write-Host "  - 방화벽 규칙 확인"
    Write-Host "  - 로그 모니터링 설정"
    Write-Host ""
    Write-Host "유용한 명령어:" -ForegroundColor Blue
    Write-Host "  - 로그 확인: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_PROD logs -f"
    Write-Host "  - 서비스 상태: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_PROD ps"
}
else {
    Write-Host "❌ 서비스 시작 실패" -ForegroundColor Red
    exit 1
}