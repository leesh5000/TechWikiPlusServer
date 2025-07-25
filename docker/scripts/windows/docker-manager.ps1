# TechWikiPlus Docker 통합 관리 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

# 디렉토리 및 BuildKit 설정
Setup-Directories
Enable-BuildKit

# 로고 출력
function Show-Logo {
    Write-Host "`n" -NoNewline
    Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║      TechWikiPlus Docker Manager 🐳        ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

# 메인 메뉴
function Show-MainMenu {
    Write-Host "메인 메뉴" -ForegroundColor Green
    Write-Host "─────────────────────────────────────────────"
    Write-Host "1) 🚀 서비스 시작"
    Write-Host "2) 🛑 서비스 종료"
    Write-Host "3) 🔄 서비스 재시작"
    Write-Host "4) 📊 서비스 상태 확인"
    Write-Host "5) 📋 로그 확인"
    Write-Host "6) 🧹 Docker 리소스 정리"
    Write-Host "7) 🔧 고급 옵션"
    Write-Host "0) 종료"
    Write-Host "─────────────────────────────────────────────"
}

# 서비스 시작 메뉴
function Start-ServicesMenu {
    Write-Host "`n서비스 시작" -ForegroundColor Green
    Write-Host "─────────────────────────────────────────────"
    Write-Host "1) 로컬 개발 환경 (전체)"
    Write-Host "2) 프로덕션 환경 (전체)"
    Write-Host "3) 인프라만 (MySQL + Redis)"
    Write-Host "0) 메인 메뉴로"
    Write-Host "─────────────────────────────────────────────"
    
    $choice = Read-Host "선택"
    
    switch ($choice) {
        "1" { & "$PSScriptRoot\build-local.ps1" }
        "2" { & "$PSScriptRoot\build-prod.ps1" }
        "3" { & "$PSScriptRoot\infra-start.ps1" }
        "0" { return }
        default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
    }
    
    Read-Host "계속하려면 Enter를 누르세요..."
}

# 서비스 종료 메뉴
function Stop-ServicesMenu {
    Write-Host "`n서비스 종료" -ForegroundColor Red
    Write-Host "─────────────────────────────────────────────"
    Write-Host "1) 로컬 환경 종료"
    Write-Host "2) 프로덕션 환경 종료"
    Write-Host "3) 인프라만 종료"
    Write-Host "4) 모든 서비스 종료"
    Write-Host "0) 메인 메뉴로"
    Write-Host "─────────────────────────────────────────────"
    
    $choice = Read-Host "선택"
    
    switch ($choice) {
        "1" { & "$PSScriptRoot\stop-local.ps1" }
        "2" { & "$PSScriptRoot\stop-prod.ps1" }
        "3" { & "$PSScriptRoot\infra-stop.ps1" }
        "4" {
            Write-Host "`n⚠️  모든 서비스를 종료합니다..." -ForegroundColor Red
            docker ps --filter "label=com.docker.compose.project" --format "table {{.Names}}\t{{.Status}}"
            
            $confirm = Read-Host "모든 서비스를 종료하시겠습니까? (y/N)"
            if ($confirm -match "^[Yy]$") {
                # 로컬 환경
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down 2>$null
                # 프로덕션 환경
                docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down 2>$null
                # 인프라만
                docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down 2>$null
                
                Write-Host "✅ 모든 서비스가 종료되었습니다." -ForegroundColor Green
            }
        }
        "0" { return }
        default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
    }
    
    Read-Host "계속하려면 Enter를 누르세요..."
}

# 상태 확인
function Check-Status {
    Write-Host "`n서비스 상태" -ForegroundColor Cyan
    Write-Host "─────────────────────────────────────────────"
    
    Write-Host "`n실행 중인 컨테이너:" -ForegroundColor Blue
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    Write-Host "`nDocker 네트워크:" -ForegroundColor Blue
    docker network ls | Select-String -Pattern "techwikiplus"
    
    Write-Host "`nDocker 볼륨:" -ForegroundColor Blue
    docker volume ls | Select-String -Pattern "(mysql|redis)"
    
    Write-Host "`n리소스 사용량:" -ForegroundColor Blue
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
    
    Read-Host "계속하려면 Enter를 누르세요..."
}

# 로그 확인 메뉴
function View-LogsMenu {
    Write-Host "`n로그 확인" -ForegroundColor Cyan
    Write-Host "─────────────────────────────────────────────"
    Write-Host "1) 모든 서비스 로그"
    Write-Host "2) User Service 로그"
    Write-Host "3) MySQL 로그"
    Write-Host "4) Redis 로그"
    Write-Host "0) 메인 메뉴로"
    Write-Host "─────────────────────────────────────────────"
    
    $choice = Read-Host "선택"
    
    # 어떤 환경인지 확인
    Write-Host "`n환경 선택:" -ForegroundColor Cyan
    Write-Host "1) 로컬 환경"
    Write-Host "2) 프로덕션 환경"
    Write-Host "3) 인프라만"
    $envChoice = Read-Host "선택"
    
    $projectName = ""
    $composeFiles = ""
    
    switch ($envChoice) {
        "1" {
            $projectName = $PROJECT_NAME_USER_SERVICE
            $composeFiles = "-f $COMPOSE_BASE -f $COMPOSE_LOCAL"
        }
        "2" {
            $projectName = $PROJECT_NAME_USER_SERVICE
            $composeFiles = "-f $COMPOSE_BASE -f $COMPOSE_PROD"
        }
        "3" {
            $projectName = $PROJECT_NAME_INFRA
            $composeFiles = "-f $COMPOSE_BASE"
        }
        default {
            Write-Host "잘못된 선택입니다." -ForegroundColor Red
            return
        }
    }
    
    switch ($choice) {
        "1" { docker-compose -p $projectName $composeFiles logs -f --tail=100 }
        "2" { docker-compose -p $projectName $composeFiles logs -f --tail=100 user-service }
        "3" { docker-compose -p $projectName $composeFiles logs -f --tail=100 mysql }
        "4" { docker-compose -p $projectName $composeFiles logs -f --tail=100 redis }
        "0" { return }
        default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
    }
}

# 고급 옵션 메뉴
function Advanced-Menu {
    Write-Host "`n고급 옵션" -ForegroundColor Magenta
    Write-Host "─────────────────────────────────────────────"
    Write-Host "1) 컨테이너 쉘 접속"
    Write-Host "2) MySQL 클라이언트 접속"
    Write-Host "3) Redis CLI 접속"
    Write-Host "4) Docker Compose 설정 검증"
    Write-Host "5) 환경변수 파일 편집"
    Write-Host "0) 메인 메뉴로"
    Write-Host "─────────────────────────────────────────────"
    
    $choice = Read-Host "선택"
    
    switch ($choice) {
        "1" {
            Write-Host "`n접속할 컨테이너:" -ForegroundColor Cyan
            docker ps --format "table {{.Names}}"
            $containerName = Read-Host "컨테이너 이름"
            docker exec -it $containerName sh
        }
        "2" {
            $password = Read-Host "MySQL 비밀번호" -AsSecureString
            $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
            $plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
            docker exec -it $CONTAINER_MYSQL mysql -u $MYSQL_USER_DEFAULT -p$plainPassword
        }
        "3" {
            $password = Read-Host "Redis 비밀번호" -AsSecureString
            $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
            $plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
            docker exec -it $CONTAINER_REDIS redis-cli -a $plainPassword
        }
        "4" {
            Write-Host "`nDocker Compose 설정 검증:" -ForegroundColor Cyan
            Write-Host "1) 로컬 환경"
            Write-Host "2) 프로덕션 환경"
            $envChoice = Read-Host "환경 선택"
            switch ($envChoice) {
                "1" { docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_LOCAL config }
                "2" { docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_PROD config }
                default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
            }
        }
        "5" {
            Write-Host "`n편집할 환경변수 파일:" -ForegroundColor Cyan
            Write-Host "1) .env.local"
            Write-Host "2) .env.prod"
            $envChoice = Read-Host "선택"
            switch ($envChoice) {
                "1" { notepad $ENV_LOCAL }
                "2" { notepad $ENV_PROD }
                default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
            }
        }
        "0" { return }
        default { Write-Host "잘못된 선택입니다." -ForegroundColor Red }
    }
    
    Read-Host "계속하려면 Enter를 누르세요..."
}

# 메인 루프
function Main {
    Clear-Host
    Show-Logo
    
    while ($true) {
        Show-MainMenu
        $choice = Read-Host "선택"
        
        switch ($choice) {
            "1" { Start-ServicesMenu }
            "2" { Stop-ServicesMenu }
            "3" { & "$PSScriptRoot\restart.ps1"; Read-Host "계속하려면 Enter를 누르세요..." }
            "4" { Check-Status }
            "5" { View-LogsMenu }
            "6" { & "$PSScriptRoot\cleanup.ps1"; Read-Host "계속하려면 Enter를 누르세요..." }
            "7" { Advanced-Menu }
            "0" {
                Write-Host "`nDocker Manager를 종료합니다. 안녕히 가세요!" -ForegroundColor Green
                exit 0
            }
            default {
                Write-Host "잘못된 선택입니다. 다시 선택해주세요." -ForegroundColor Red
                Read-Host "계속하려면 Enter를 누르세요..."
            }
        }
        
        Clear-Host
        Show-Logo
    }
}

# 스크립트 실행
Main