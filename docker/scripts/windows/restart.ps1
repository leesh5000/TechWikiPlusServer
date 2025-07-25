# Docker 서비스 재시작 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🔄 TechWikiPlus 서비스 재시작" -ForegroundColor Blue

# 디렉토리 설정
Setup-Directories

# 환경 선택
Write-Host "재시작할 환경을 선택하세요:"
Write-Host "1) 로컬 개발 환경"
Write-Host "2) 프로덕션 환경"
Write-Host "0) 취소"

$choice = Read-Host "선택 [0-2]"

switch ($choice) {
    "1" {
        Write-Host "로컬 개발 환경 재시작 중..." -ForegroundColor Blue
        
        # 서비스별 재시작 옵션
        Write-Host ""
        Write-Host "재시작 옵션:"
        Write-Host "1) 전체 서비스 재시작"
        Write-Host "2) User Service만 재시작"
        Write-Host "3) MySQL만 재시작"
        Write-Host "4) Redis만 재시작"
        
        $restartOption = Read-Host "선택 [1-4]"
        
        switch ($restartOption) {
            "1" {
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" restart
            }
            "2" {
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" restart user-service
            }
            "3" {
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" restart mysql
            }
            "4" {
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" restart redis
            }
            default {
                Write-Host "잘못된 선택입니다." -ForegroundColor Red
                exit 1
            }
        }
    }
    "2" {
        Write-Host "⚠️  경고: 프로덕션 서비스를 재시작하면 일시적으로 서비스가 중단됩니다!" -ForegroundColor Yellow
        $confirm = Read-Host "계속하시겠습니까? (yes/N)"
        
        if ($confirm -eq "yes") {
            Write-Host "프로덕션 환경 재시작 중..." -ForegroundColor Blue
            
            # Rolling restart 시뮬레이션
            Write-Host "Rolling restart 수행 중..." -ForegroundColor Blue
            
            # 먼저 새 컨테이너 시작
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" up -d --no-deps user-service
            
            # 헬스체크 대기
            Write-Host "헬스체크 대기 중 ($HEALTH_CHECK_WAIT초)..." -ForegroundColor Blue
            Start-Sleep -Seconds $HEALTH_CHECK_WAIT
            
            # 이전 컨테이너 정리
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" restart
        }
    }
    "0" {
        Write-Host "취소되었습니다." -ForegroundColor Green
        exit 0
    }
    default {
        Write-Host "잘못된 선택입니다." -ForegroundColor Red
        exit 1
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 서비스가 재시작되었습니다!" -ForegroundColor Green
    
    # 서비스 상태 확인
    Write-Host "`n서비스 상태:" -ForegroundColor Blue
    docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" ps
}
else {
    Write-Host "❌ 재시작 실패" -ForegroundColor Red
    exit 1
}