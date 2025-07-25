# TechWikiPlus Docker 공통 설정 파일 (PowerShell)
# 모든 Docker 스크립트에서 사용하는 공통 변수들을 정의합니다.

# Docker 프로젝트 이름
$global:PROJECT_NAME_USER_SERVICE = "techwikiplus-server-user-service"
$global:PROJECT_NAME_INFRA = "techwikiplus-server-infra"

# Docker Compose 파일 경로
$global:COMPOSE_BASE = "docker/compose/docker-compose.base.yml"
$global:COMPOSE_LOCAL = "docker/compose/docker-compose.local.yml"
$global:COMPOSE_PROD = "docker/compose/docker-compose.prod.yml"

# 환경 변수 파일
$global:ENV_LOCAL = ".env.local"
$global:ENV_PROD = ".env.prod"
$global:ENV_LOCAL_EXAMPLE = "docker/env/.env.local.example"
$global:ENV_PROD_EXAMPLE = "docker/env/.env.prod.example"

# 컨테이너 이름
$global:CONTAINER_MYSQL = "techwikiplus-mysql"
$global:CONTAINER_REDIS = "techwikiplus-redis"
$global:CONTAINER_USER_SERVICE = "techwikiplus-user-service"

# 포트 설정
$global:MYSQL_PORT_DEFAULT = "13306"
$global:REDIS_PORT_DEFAULT = "16379"
$global:USER_SERVICE_PORT = "9000"

# 기본값 설정
$global:MYSQL_DATABASE_DEFAULT = "techwikiplus"
$global:MYSQL_USER_DEFAULT = "techwikiplus"

# 타임아웃 설정
$global:GRACEFUL_SHUTDOWN_TIMEOUT = "10"
$global:HEALTH_CHECK_WAIT = "30"

# 문자열 마스킹 함수
function Mask-String {
    param(
        [string]$str
    )
    
    $len = $str.Length
    
    if ($len -le 4) {
        return "****"
    }
    elseif ($len -le 8) {
        return $str.Substring(0, 2) + "****" + $str.Substring($len - 2)
    }
    else {
        return $str.Substring(0, 2) + "****" + $str.Substring($len - 4)
    }
}

# 스크립트 디렉토리 및 프로젝트 루트 설정 함수
function Setup-Directories {
    $global:SCRIPT_DIR = Split-Path -Parent $MyInvocation.PSCommandPath
    $global:PROJECT_ROOT = (Get-Item $SCRIPT_DIR).Parent.Parent.FullName
    Set-Location $PROJECT_ROOT
}

# Docker BuildKit 활성화 함수
function Enable-BuildKit {
    $env:DOCKER_BUILDKIT = "1"
    $env:COMPOSE_DOCKER_CLI_BUILD = "1"
}

# 환경 파일 확인 함수
function Check-EnvFile {
    param(
        [string]$envFile,
        [string]$exampleFile
    )
    
    if (-not (Test-Path $envFile)) {
        Write-Host "⚠️  $envFile 파일이 없습니다. $exampleFile을 복사합니다." -ForegroundColor Yellow
        Copy-Item $exampleFile $envFile
        return $false
    }
    return $true
}

# 서비스 정보 출력 함수
function Show-ServiceInfo {
    Write-Host "`n서비스 접속 정보:" -ForegroundColor Cyan
    Write-Host "─────────────────────────────────────────────"
    Write-Host "  User Service API: http://localhost:$USER_SERVICE_PORT"
    Write-Host "  MySQL: localhost:$($env:MYSQL_PORT ?? $MYSQL_PORT_DEFAULT)"
    Write-Host "  Redis: localhost:$($env:REDIS_PORT ?? $REDIS_PORT_DEFAULT)"
    Write-Host "─────────────────────────────────────────────"
}

# 환경 정보 출력 함수
function Show-EnvInfo {
    param(
        [string]$envFile
    )
    
    if (Test-Path $envFile) {
        # 환경 변수 파일 읽기
        $envContent = Get-Content $envFile
        $envVars = @{}
        foreach ($line in $envContent) {
            if ($line -match '^([^=]+)=(.*)$') {
                $envVars[$matches[1]] = $matches[2]
            }
        }
        
        Write-Host "`n환경 설정 정보:" -ForegroundColor Cyan
        Write-Host "─────────────────────────────────────────────"
        
        # MySQL 정보
        Write-Host "MySQL:" -ForegroundColor Blue
        Write-Host "  - Host: localhost:$($envVars['MYSQL_PORT'] ?? $MYSQL_PORT_DEFAULT)"
        Write-Host "  - Database: $($envVars['MYSQL_DATABASE'] ?? $MYSQL_DATABASE_DEFAULT)"
        Write-Host "  - User: $($envVars['MYSQL_USER'] ?? $MYSQL_USER_DEFAULT)"
        if ($envVars['MYSQL_PASSWORD']) {
            Write-Host "  - Password: $(Mask-String $envVars['MYSQL_PASSWORD'])"
        }
        
        # Redis 정보
        Write-Host "`nRedis:" -ForegroundColor Blue
        Write-Host "  - Host: localhost:$($envVars['REDIS_PORT'] ?? $REDIS_PORT_DEFAULT)"
        if ($envVars['REDIS_PASSWORD']) {
            Write-Host "  - Password: $(Mask-String $envVars['REDIS_PASSWORD'])"
        }
        
        # JWT 정보
        Write-Host "`nJWT:" -ForegroundColor Blue
        if ($envVars['JWT_SECRET']) {
            Write-Host "  - Secret: $(Mask-String $envVars['JWT_SECRET'])"
        }
        Write-Host "  - Access Token Expiration: $($envVars['JWT_ACCESS_TOKEN_EXPIRATION'] ?? '3600000')ms"
        Write-Host "  - Refresh Token Expiration: $($envVars['JWT_REFRESH_TOKEN_EXPIRATION'] ?? '604800000')ms"
        
        # Mail 정보
        Write-Host "`nMail:" -ForegroundColor Blue
        Write-Host "  - Type: $($envVars['SPRING_MAIL_TYPE'] ?? 'console')"
        if ($envVars['SPRING_MAIL_TYPE'] -eq 'smtp') {
            Write-Host "  - Host: $($envVars['SPRING_MAIL_HOST'] ?? 'localhost')"
            Write-Host "  - Port: $($envVars['SPRING_MAIL_PORT'] ?? '1025')"
            if ($envVars['MAIL_USERNAME']) {
                Write-Host "  - Username: $($envVars['MAIL_USERNAME'])"
            }
            if ($envVars['MAIL_PASSWORD']) {
                Write-Host "  - Password: $(Mask-String $envVars['MAIL_PASSWORD'])"
            }
        }
        
        Write-Host "─────────────────────────────────────────────"
    }
}