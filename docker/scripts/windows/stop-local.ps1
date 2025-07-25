# 로컬 개발 환경 종료 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🛑 TechWikiPlus 로컬 개발 환경 종료" -ForegroundColor Yellow

# 디렉토리 설정
Setup-Directories

# 실행 중인 컨테이너 확인
Write-Host "현재 실행 중인 서비스:" -ForegroundColor Blue
docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" ps

Write-Host ""
$confirm = Read-Host "서비스를 종료하시겠습니까? (y/N)"

if ($confirm -match "^[Yy]$") {
    Write-Host "서비스 종료 중..." -ForegroundColor Blue
    
    docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 모든 서비스가 종료되었습니다." -ForegroundColor Green
        
        # 볼륨 제거 옵션
        Write-Host ""
        $removeVolumes = Read-Host "데이터 볼륨도 제거하시겠습니까? (y/N)"
        
        if ($removeVolumes -match "^[Yy]$") {
            Write-Host "⚠️  경고: 모든 데이터가 삭제됩니다!" -ForegroundColor Yellow
            $confirmVolumes = Read-Host "정말로 계속하시겠습니까? (y/N)"
            
            if ($confirmVolumes -match "^[Yy]$") {
                Write-Host "볼륨 제거 중..." -ForegroundColor Blue
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down -v
                Write-Host "✅ 볼륨이 제거되었습니다." -ForegroundColor Green
            }
        }
    }
    else {
        Write-Host "❌ 서비스 종료 실패" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "취소되었습니다." -ForegroundColor Green
}