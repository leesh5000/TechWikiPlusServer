# 프로덕션 환경 종료 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🛑 TechWikiPlus 프로덕션 환경 종료" -ForegroundColor Yellow

# 디렉토리 설정
Setup-Directories

# 실행 중인 컨테이너 확인
Write-Host "현재 실행 중인 프로덕션 서비스:" -ForegroundColor Blue
docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" ps

Write-Host ""
Write-Host "⚠️  경고: 프로덕션 서비스를 종료하면 서비스가 중단됩니다!" -ForegroundColor Yellow
$confirm = Read-Host "정말로 프로덕션 서비스를 종료하시겠습니까? (yes/N)"

if ($confirm -eq "yes") {
    Write-Host "프로덕션 서비스 종료 중..." -ForegroundColor Blue
    
    # Graceful shutdown을 위한 대기 시간
    Write-Host "Graceful shutdown을 위해 $GRACEFUL_SHUTDOWN_TIMEOUT초 대기 중..." -ForegroundColor Blue
    docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" stop -t $GRACEFUL_SHUTDOWN_TIMEOUT
    
    # 컨테이너 제거
    docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 프로덕션 서비스가 안전하게 종료되었습니다." -ForegroundColor Green
        Write-Host "참고: 데이터 볼륨은 보존되었습니다." -ForegroundColor Yellow
    }
    else {
        Write-Host "❌ 서비스 종료 실패" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "취소되었습니다." -ForegroundColor Green
    Write-Host "프로덕션 서비스가 계속 실행됩니다." -ForegroundColor Blue
}