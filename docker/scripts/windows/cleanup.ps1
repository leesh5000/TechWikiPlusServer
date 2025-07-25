# Docker 리소스 정리 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🧹 Docker 리소스 정리" -ForegroundColor Yellow

# 디렉토리 설정
Setup-Directories

# 정리 옵션 선택
Write-Host "정리할 항목을 선택하세요:"
Write-Host "1) 컨테이너만 중지 및 제거"
Write-Host "2) 컨테이너 + 볼륨 제거 (데이터 삭제)"
Write-Host "3) 컨테이너 + 이미지 제거"
Write-Host "4) 전체 정리 (컨테이너 + 볼륨 + 이미지)"
Write-Host "5) 시스템 전체 정리 (미사용 리소스 모두)"
Write-Host "0) 취소"

$choice = Read-Host "선택 [0-5]"

switch ($choice) {
    "1" {
        Write-Host "컨테이너 중지 및 제거 중..." -ForegroundColor Blue
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down 2>$null
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down 2>$null
        docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down 2>$null
    }
    "2" {
        Write-Host "⚠️  경고: 모든 데이터가 삭제됩니다!" -ForegroundColor Yellow
        $confirm = Read-Host "계속하시겠습니까? (y/N)"
        if ($confirm -match "^[Yy]$") {
            Write-Host "컨테이너 및 볼륨 제거 중..." -ForegroundColor Blue
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down -v 2>$null
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down -v 2>$null
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v 2>$null
        }
    }
    "3" {
        Write-Host "컨테이너 및 이미지 제거 중..." -ForegroundColor Blue
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down --rmi all 2>$null
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down --rmi all 2>$null
        docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down --rmi all 2>$null
    }
    "4" {
        Write-Host "⚠️  경고: 모든 데이터와 이미지가 삭제됩니다!" -ForegroundColor Yellow
        $confirm = Read-Host "계속하시겠습니까? (y/N)"
        if ($confirm -match "^[Yy]$") {
            Write-Host "전체 정리 중..." -ForegroundColor Blue
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down -v --rmi all 2>$null
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down -v --rmi all 2>$null
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v --rmi all 2>$null
        }
    }
    "5" {
        Write-Host "⚠️  경고: 시스템의 모든 미사용 Docker 리소스가 삭제됩니다!" -ForegroundColor Yellow
        $confirm = Read-Host "계속하시겠습니까? (y/N)"
        if ($confirm -match "^[Yy]$") {
            Write-Host "시스템 정리 중..." -ForegroundColor Blue
            docker system prune -a --volumes -f
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

Write-Host "✅ 정리 완료!" -ForegroundColor Green

# 디스크 사용량 표시
Write-Host "`n현재 Docker 디스크 사용량:" -ForegroundColor Blue
docker system df