#!/bin/bash

# 프로덕션 환경을 위한 Docker 실행 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${GREEN}🚀 TechWikiPlus 프로덕션 환경 시작${NC}"

# 디렉토리 설정
setup_directories

# .env.prod 파일 확인
if [ ! -f "$ENV_PROD" ]; then
    echo -e "${RED}❌ $ENV_PROD 파일이 없습니다!${NC}"
    echo -e "${YELLOW}$ENV_PROD_EXAMPLE을 복사하고 프로덕션 값을 설정하세요:${NC}"
    echo "  cp $ENV_PROD_EXAMPLE $ENV_PROD"
    exit 1
fi

# 필수 환경 변수 확인
required_vars=(
    "USER_SERVICE_IMAGE"
    "JWT_SECRET"
    "MYSQL_PASSWORD"
    "REDIS_PASSWORD"
    "MAIL_USERNAME"
    "MAIL_PASSWORD"
)

missing_vars=()
for var in "${required_vars[@]}"; do
    if ! grep -q "^${var}=" "$ENV_PROD" || grep -q "^${var}=<" "$ENV_PROD"; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -ne 0 ]; then
    echo -e "${RED}❌ 필수 환경 변수가 설정되지 않았습니다:${NC}"
    printf '%s\n' "${missing_vars[@]}"
    exit 1
fi

# Docker Compose 명령어 실행
echo -e "${GREEN}🔄 서비스 시작 중...${NC}"

docker-compose \
    -p "$PROJECT_NAME_USER_SERVICE" \
    -f "$COMPOSE_BASE" \
    -f "$COMPOSE_PROD" \
    --env-file "$ENV_PROD" \
    up -d

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 프로덕션 서비스가 시작되었습니다!${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  프로덕션 환경 주의사항:${NC}"
    echo -e "  - SSL/TLS 인증서 설정 필요"
    echo -e "  - 방화벽 규칙 확인"
    echo -e "  - 로그 모니터링 설정"
    echo ""
    echo -e "${BLUE}유용한 명령어:${NC}"
    echo -e "  - 로그 확인: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_PROD logs -f"
    echo -e "  - 서비스 상태: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_PROD ps"
else
    echo -e "${RED}❌ 서비스 시작 실패${NC}"
    exit 1
fi