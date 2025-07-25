# 인프라(MySQL, Redis)만 실행하는 스크립트

# 공통 설정 로드
. "$PSScriptRoot\common.ps1"

Write-Host "🗄️  TechWikiPlus 인프라 서비스 시작" -ForegroundColor Green
Write-Host "MySQL과 Redis만 실행합니다." -ForegroundColor Blue

# 디렉토리 및 BuildKit 설정
Setup-Directories
Enable-BuildKit

# 환경 선택
Write-Host ""
Write-Host "환경을 선택하세요:"
Write-Host "1) 로컬 개발 환경 설정 사용 (.env.local)"
Write-Host "2) 프로덕션 환경 설정 사용 (.env.prod)"
Write-Host "3) 기본값 사용"

$envChoice = Read-Host "선택 [1-3]"

$ENV_FILE = ""
switch ($envChoice) {
    "1" {
        Check-EnvFile $ENV_LOCAL $ENV_LOCAL_EXAMPLE | Out-Null
        $ENV_FILE = "--env-file $ENV_LOCAL"
        Write-Host "로컬 환경 설정을 사용합니다." -ForegroundColor Blue
    }
    "2" {
        if (-not (Test-Path $ENV_PROD)) {
            Write-Host "❌ $ENV_PROD 파일이 없습니다!" -ForegroundColor Red
            Write-Host "먼저 프로덕션 환경 설정을 생성하세요:" -ForegroundColor Yellow
            Write-Host "  cp $ENV_PROD_EXAMPLE $ENV_PROD"
            exit 1
        }
        $ENV_FILE = "--env-file $ENV_PROD"
        Write-Host "프로덕션 환경 설정을 사용합니다." -ForegroundColor Blue
    }
    "3" {
        Write-Host "기본 설정값을 사용합니다." -ForegroundColor Blue
    }
    default {
        Write-Host "잘못된 선택입니다." -ForegroundColor Red
        exit 1
    }
}

# 인프라 서비스 시작
Write-Host "`n📦 인프라 서비스 시작 중..." -ForegroundColor Green

if ($ENV_FILE) {
    $cmd = "docker-compose -p `"$PROJECT_NAME_INFRA`" -f `"$COMPOSE_BASE`" $ENV_FILE up -d"
}
else {
    $cmd = "docker-compose -p `"$PROJECT_NAME_INFRA`" -f `"$COMPOSE_BASE`" up -d"
}

Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 인프라 서비스가 시작되었습니다!" -ForegroundColor Green
    Write-Host ""
    Write-Host "서비스 정보:" -ForegroundColor Green
    Write-Host "  - MySQL: localhost:$($env:MYSQL_PORT ?? $MYSQL_PORT_DEFAULT)"
    Write-Host "    - Database: $($env:MYSQL_DATABASE ?? $MYSQL_DATABASE_DEFAULT)"
    Write-Host "    - User: $($env:MYSQL_USER ?? $MYSQL_USER_DEFAULT)"
    
    # 환경 파일에서 패스워드 읽기
    if ($ENV_FILE) {
        $envPath = $ENV_FILE -replace '--env-file ', ''
        if (Test-Path $envPath) {
            $envContent = Get-Content $envPath
            $mysqlPass = ($envContent | Select-String -Pattern '^MYSQL_PASSWORD=(.*)$').Matches[0].Groups[1].Value
            if ($mysqlPass) {
                Write-Host "    - Password: $(Mask-String $mysqlPass)"
            }
        }
    }
    
    Write-Host "  - Redis: localhost:$($env:REDIS_PORT ?? $REDIS_PORT_DEFAULT)"
    
    Write-Host ""
    Write-Host "연결 테스트:" -ForegroundColor Blue
    Write-Host "  - MySQL: mysql -h localhost -P $($env:MYSQL_PORT ?? $MYSQL_PORT_DEFAULT) -u $($env:MYSQL_USER ?? $MYSQL_USER_DEFAULT) -p"
    Write-Host "  - Redis: redis-cli -h localhost -p $($env:REDIS_PORT ?? $REDIS_PORT_DEFAULT) -a <password>"
    Write-Host ""
    Write-Host "상태 확인: docker-compose -p $PROJECT_NAME_INFRA -f $COMPOSE_BASE ps" -ForegroundColor Yellow
    Write-Host "로그 확인: docker-compose -p $PROJECT_NAME_INFRA -f $COMPOSE_BASE logs -f" -ForegroundColor Yellow
}
else {
    Write-Host "❌ 인프라 서비스 시작 실패" -ForegroundColor Red
    exit 1
}