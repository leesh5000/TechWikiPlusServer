# 인프라(MySQL, Redis) 종료 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🛑 TechWikiPlus 인프라 서비스 종료" -ForegroundColor Yellow

# 디렉토리 설정
Setup-Directories

# 실행 중인 인프라 서비스 확인
Write-Host "현재 실행 중인 인프라 서비스:" -ForegroundColor Blue
docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" ps

# 실행 중인 서비스가 있는지 확인
$runningServices = docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" ps -q
if (-not $runningServices) {
    Write-Host "`n실행 중인 인프라 서비스가 없습니다." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
$confirm = Read-Host "인프라 서비스를 종료하시겠습니까? (y/N)"

if ($confirm -match "^[Yy]$") {
    Write-Host "인프라 서비스 종료 중..." -ForegroundColor Blue
    
    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 인프라 서비스가 종료되었습니다." -ForegroundColor Green
        
        # 볼륨 제거 옵션
        Write-Host ""
        Write-Host "⚠️  데이터 볼륨 관리" -ForegroundColor Yellow
        Write-Host "1) 데이터 유지 (기본)"
        Write-Host "2) MySQL 데이터만 삭제"
        Write-Host "3) Redis 데이터만 삭제"
        Write-Host "4) 모든 데이터 삭제"
        
        $volumeChoice = Read-Host "선택 [1-4] (기본: 1)"
        if (-not $volumeChoice) { $volumeChoice = "1" }
        
        switch ($volumeChoice) {
            "1" {
                Write-Host "데이터가 유지됩니다." -ForegroundColor Green
            }
            "2" {
                Write-Host "⚠️  경고: MySQL 데이터가 삭제됩니다!" -ForegroundColor Yellow
                $confirmMysql = Read-Host "정말로 계속하시겠습니까? (y/N)"
                
                if ($confirmMysql -match "^[Yy]$") {
                    docker volume rm techwikiplus_mysql-data 2>$null
                    docker volume rm (docker volume ls -q | Select-String -Pattern 'mysql-data') 2>$null
                    Write-Host "✅ MySQL 데이터가 삭제되었습니다." -ForegroundColor Green
                }
            }
            "3" {
                Write-Host "⚠️  경고: Redis 데이터가 삭제됩니다!" -ForegroundColor Yellow
                $confirmRedis = Read-Host "정말로 계속하시겠습니까? (y/N)"
                
                if ($confirmRedis -match "^[Yy]$") {
                    docker volume rm techwikiplus_redis-data 2>$null
                    docker volume rm (docker volume ls -q | Select-String -Pattern 'redis-data') 2>$null
                    Write-Host "✅ Redis 데이터가 삭제되었습니다." -ForegroundColor Green
                }
            }
            "4" {
                Write-Host "⚠️  경고: 모든 데이터가 삭제됩니다!" -ForegroundColor Yellow
                $confirmAll = Read-Host "정말로 계속하시겠습니까? (y/N)"
                
                if ($confirmAll -match "^[Yy]$") {
                    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v
                    Write-Host "✅ 모든 데이터가 삭제되었습니다." -ForegroundColor Green
                }
            }
            default {
                Write-Host "데이터가 유지됩니다." -ForegroundColor Green
            }
        }
        
        # 디스크 사용량 표시
        Write-Host "`nDocker 볼륨 사용량:" -ForegroundColor Blue
        docker volume ls | Select-String -Pattern "(mysql|redis)"
        
    }
    else {
        Write-Host "❌ 인프라 서비스 종료 실패" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "취소되었습니다." -ForegroundColor Green
}