#!/bin/bash

# 인프라(MySQL, Redis)만 실행하는 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${GREEN}🗄️  TechWikiPlus 인프라 서비스 시작${NC}"
echo -e "${BLUE}MySQL과 Redis만 실행합니다.${NC}"

# 디렉토리 및 BuildKit 설정
setup_directories
enable_buildkit

# 환경 선택
echo ""
echo "환경을 선택하세요:"
echo "1) 로컬 개발 환경 설정 사용 (.env.local)"
echo "2) 프로덕션 환경 설정 사용 (.env.prod)"
echo "3) 기본값 사용"

read -p "선택 [1-3]: " env_choice

ENV_FILE=""
case $env_choice in
    1)
        check_env_file "$ENV_LOCAL" "$ENV_LOCAL_EXAMPLE"
        ENV_FILE="--env-file $ENV_LOCAL"
        echo -e "${BLUE}로컬 환경 설정을 사용합니다.${NC}"
        ;;
    2)
        if [ ! -f "$ENV_PROD" ]; then
            echo -e "${RED}❌ $ENV_PROD 파일이 없습니다!${NC}"
            echo -e "${YELLOW}먼저 프로덕션 환경 설정을 생성하세요:${NC}"
            echo "  cp $ENV_PROD_EXAMPLE $ENV_PROD"
            exit 1
        fi
        ENV_FILE="--env-file $ENV_PROD"
        echo -e "${BLUE}프로덕션 환경 설정을 사용합니다.${NC}"
        ;;
    3)
        echo -e "${BLUE}기본 설정값을 사용합니다.${NC}"
        ;;
    *)
        echo -e "${RED}잘못된 선택입니다.${NC}"
        exit 1
        ;;
esac

# 인프라 서비스 시작
echo -e "\n${GREEN}📦 인프라 서비스 시작 중...${NC}"

if [ -n "$ENV_FILE" ]; then
    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" $ENV_FILE up -d
else
    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" up -d
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 인프라 서비스가 시작되었습니다!${NC}"
    echo ""
    echo -e "${GREEN}서비스 정보:${NC}"
    echo -e "  - MySQL: localhost:${MYSQL_PORT:-$MYSQL_PORT_DEFAULT}"
    echo -e "    - Database: ${MYSQL_DATABASE:-$MYSQL_DATABASE_DEFAULT}"
    echo -e "    - User: ${MYSQL_USER:-$MYSQL_USER_DEFAULT}"
    
    # 환경 파일에서 패스워드 읽기
    if [ -n "$ENV_FILE" ]; then
        source ${ENV_FILE#--env-file }
        [ -n "$MYSQL_PASSWORD" ] && echo -e "    - Password: $(mask_string "$MYSQL_PASSWORD")"
    fi
    
    echo -e "  - Redis: localhost:${REDIS_PORT:-$REDIS_PORT_DEFAULT}"
    [ -n "$REDIS_PASSWORD" ] && echo -e "    - Password: $(mask_string "$REDIS_PASSWORD")"
    
    echo ""
    echo -e "${BLUE}연결 테스트:${NC}"
    echo -e "  - MySQL: mysql -h localhost -P ${MYSQL_PORT:-$MYSQL_PORT_DEFAULT} -u ${MYSQL_USER:-$MYSQL_USER_DEFAULT} -p"
    echo -e "  - Redis: redis-cli -h localhost -p ${REDIS_PORT:-$REDIS_PORT_DEFAULT} -a <password>"
    echo ""
    echo -e "${YELLOW}상태 확인: docker-compose -p $PROJECT_NAME_INFRA -f $COMPOSE_BASE ps${NC}"
    echo -e "${YELLOW}로그 확인: docker-compose -p $PROJECT_NAME_INFRA -f $COMPOSE_BASE logs -f${NC}"
else
    echo -e "${RED}❌ 인프라 서비스 시작 실패${NC}"
    exit 1
fi