#!/bin/bash

# 로컬 개발 환경을 위한 Docker 빌드 및 실행 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${GREEN}🐳 TechWikiPlus 로컬 개발 환경 시작${NC}"

# 디렉토리 및 BuildKit 설정
setup_directories
enable_buildkit

# .env.local 파일 확인
if ! check_env_file "$ENV_LOCAL" "$ENV_LOCAL_EXAMPLE"; then
    echo -e "${RED}❗ ${ENV_LOCAL} 파일을 수정해주세요 (필요시).${NC}"
fi

# Docker Compose 명령어 실행
echo -e "${GREEN}📦 이미지 빌드 및 서비스 시작 중...${NC}"

docker-compose \
    -p "$PROJECT_NAME_USER_SERVICE" \
    -f "$COMPOSE_BASE" \
    -f "$COMPOSE_LOCAL" \
    --env-file "$ENV_LOCAL" \
    up -d --build

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 모든 서비스가 시작되었습니다!${NC}"
    show_service_info
    show_env_info "$ENV_LOCAL"
    
    echo -e "${BLUE}유용한 명령어:${NC}"
    echo -e "  - 로그 확인: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_LOCAL logs -f"
    echo -e "  - 서비스 중지: docker-compose -p $PROJECT_NAME_USER_SERVICE -f $COMPOSE_BASE -f $COMPOSE_LOCAL down"
    echo -e "  - 테스트 실행: ./gradlew test"
else
    echo -e "${RED}❌ 서비스 시작 실패${NC}"
    exit 1
fi