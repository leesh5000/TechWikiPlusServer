#!/bin/bash

# TechWikiPlus Docker 공통 설정 파일
# 모든 Docker 스크립트에서 사용하는 공통 변수들을 정의합니다.

# 색상 정의
export RED='\033[0;31m'
export GREEN='\033[0;32m'
export YELLOW='\033[1;33m'
export BLUE='\033[0;34m'
export CYAN='\033[0;36m'
export MAGENTA='\033[0;35m'
export NC='\033[0m' # No Color

# Docker 프로젝트 이름
export PROJECT_NAME_USER_SERVICE="techwikiplus-server-user-service"
export PROJECT_NAME_INFRA="techwikiplus-server-infra"

# Docker Compose 파일 경로
export COMPOSE_BASE="docker/compose/docker-compose.base.yml"
export COMPOSE_LOCAL="docker/compose/docker-compose.local.yml"
export COMPOSE_PROD="docker/compose/docker-compose.prod.yml"

# 환경 변수 파일
export ENV_LOCAL=".env.local"
export ENV_PROD=".env.prod"
export ENV_LOCAL_EXAMPLE="docker/env/.env.local.example"
export ENV_PROD_EXAMPLE="docker/env/.env.prod.example"

# 컨테이너 이름
export CONTAINER_MYSQL="techwikiplus-mysql"
export CONTAINER_REDIS="techwikiplus-redis"
export CONTAINER_USER_SERVICE="techwikiplus-user-service"

# 포트 설정
export MYSQL_PORT_DEFAULT="13306"
export REDIS_PORT_DEFAULT="16379"
export USER_SERVICE_PORT="9000"

# 기본값 설정
export MYSQL_DATABASE_DEFAULT="techwikiplus"
export MYSQL_USER_DEFAULT="techwikiplus"

# 타임아웃 설정
export GRACEFUL_SHUTDOWN_TIMEOUT="10"
export HEALTH_CHECK_WAIT="30"

# 문자열 마스킹 함수
mask_string() {
    local str="$1"
    local len=${#str}
    
    if [ $len -le 4 ]; then
        echo "****"
    elif [ $len -le 8 ]; then
        echo "${str:0:2}****${str: -2}"
    else
        echo "${str:0:2}****${str: -4}"
    fi
}

# 스크립트 디렉토리 및 프로젝트 루트 설정 함수
setup_directories() {
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"
    cd "$PROJECT_ROOT"
}

# Docker BuildKit 활성화 함수
enable_buildkit() {
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
}

# 환경 파일 확인 함수
check_env_file() {
    local env_file="$1"
    local example_file="$2"
    
    if [ ! -f "$env_file" ]; then
        echo -e "${YELLOW}⚠️  $env_file 파일이 없습니다. $example_file을 복사합니다.${NC}"
        cp "$example_file" "$env_file"
        return 1
    fi
    return 0
}

# 서비스 정보 출력 함수
show_service_info() {
    echo -e "\n${CYAN}서비스 접속 정보:${NC}"
    echo "─────────────────────────────────────────────"
    echo "  User Service API: http://localhost:${USER_SERVICE_PORT}"
    echo "  MySQL: localhost:${MYSQL_PORT:-$MYSQL_PORT_DEFAULT}"
    echo "  Redis: localhost:${REDIS_PORT:-$REDIS_PORT_DEFAULT}"
    echo "─────────────────────────────────────────────"
}

# 환경 정보 출력 함수
show_env_info() {
    local env_file="$1"
    
    if [ -f "$env_file" ]; then
        source "$env_file"
        
        echo -e "\n${CYAN}환경 설정 정보:${NC}"
        echo "─────────────────────────────────────────────"
        
        # MySQL 정보
        echo -e "${BLUE}MySQL:${NC}"
        echo "  - Host: localhost:${MYSQL_PORT:-$MYSQL_PORT_DEFAULT}"
        echo "  - Database: ${MYSQL_DATABASE:-$MYSQL_DATABASE_DEFAULT}"
        echo "  - User: ${MYSQL_USER:-$MYSQL_USER_DEFAULT}"
        [ -n "$MYSQL_PASSWORD" ] && echo "  - Password: $(mask_string "$MYSQL_PASSWORD")"
        
        # Redis 정보
        echo -e "\n${BLUE}Redis:${NC}"
        echo "  - Host: localhost:${REDIS_PORT:-$REDIS_PORT_DEFAULT}"
        [ -n "$REDIS_PASSWORD" ] && echo "  - Password: $(mask_string "$REDIS_PASSWORD")"
        
        # JWT 정보
        echo -e "\n${BLUE}JWT:${NC}"
        [ -n "$JWT_SECRET" ] && echo "  - Secret: $(mask_string "$JWT_SECRET")"
        echo "  - Access Token Expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:-3600000}ms"
        echo "  - Refresh Token Expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:-604800000}ms"
        
        # Mail 정보
        echo -e "\n${BLUE}Mail:${NC}"
        echo "  - Type: ${SPRING_MAIL_TYPE:-console}"
        if [ "${SPRING_MAIL_TYPE}" = "smtp" ]; then
            echo "  - Host: ${SPRING_MAIL_HOST:-localhost}"
            echo "  - Port: ${SPRING_MAIL_PORT:-1025}"
            [ -n "$MAIL_USERNAME" ] && echo "  - Username: ${MAIL_USERNAME}"
            [ -n "$MAIL_PASSWORD" ] && echo "  - Password: $(mask_string "$MAIL_PASSWORD")"
        fi
        
        echo "─────────────────────────────────────────────"
    fi
}