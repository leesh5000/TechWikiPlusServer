# 로컬 개발 환경을 위한 Docker 빌드 및 실행 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🐳 TechWikiPlus 로컬 개발 환경 시작" -ForegroundColor Green

# 디렉토리 및 BuildKit 설정
Setup-Directories
Enable-BuildKit

# .env.local 파일 확인
if (-not (Check-EnvFile $ENV_LOCAL $ENV_LOCAL_EXAMPLE)) {
    Write-Host "❗ $ENV_LOCAL 파일을 수정해주세요 (필요시)." -ForegroundColor Red
}

# Docker Compose 명령어 실행
Write-Host "📦 이미지 빌드 및 서비스 시작 중..." -ForegroundColor Green

$cmd = "docker-compose -p `"$PROJECT_NAME_USER_SERVICE`" -f `"$COMPOSE_BASE`" -f `"$COMPOSE_LOCAL`" --env-file `"$ENV_LOCAL`" up -d --build"
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 모든 서비스가 시작되었습니다!" -ForegroundColor Green
    Show-ServiceInfo
    Show-EnvInfo $ENV_LOCAL
    
    Write-Host "`n유용한 명령어:" -ForegroundColor Blue
    Write-Host "  - 로그 확인: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_LOCAL logs -f"
    Write-Host "  - 서비스 중지: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_LOCAL down"
    Write-Host "  - 테스트 실행: .\gradlew test"
}
else {
    Write-Host "❌ 서비스 시작 실패" -ForegroundColor Red
    exit 1
}