#!/bin/bash

# 프로덕션 환경 종료 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${YELLOW}🛑 TechWikiPlus 프로덕션 환경 종료${NC}"

# 디렉토리 설정
setup_directories

# 실행 중인 컨테이너 확인
echo -e "${BLUE}현재 실행 중인 프로덕션 서비스:${NC}"
docker-compose \
    -p "$PROJECT_NAME_USER_SERVICE" \
    -f "$COMPOSE_BASE" \
    -f "$COMPOSE_PROD" \
    ps

echo ""
echo -e "${YELLOW}⚠️  경고: 프로덕션 서비스를 종료하면 서비스가 중단됩니다!${NC}"
read -p "정말로 프로덕션 서비스를 종료하시겠습니까? (yes/N): " confirm

if [[ $confirm == "yes" ]]; then
    echo -e "${BLUE}프로덕션 서비스 종료 중...${NC}"
    
    # Graceful shutdown을 위한 대기 시간
    echo -e "${BLUE}Graceful shutdown을 위해 ${GRACEFUL_SHUTDOWN_TIMEOUT}초 대기 중...${NC}"
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_PROD" \
        stop -t "$GRACEFUL_SHUTDOWN_TIMEOUT"
    
    # 컨테이너 제거
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_PROD" \
        down
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 프로덕션 서비스가 안전하게 종료되었습니다.${NC}"
        echo -e "${YELLOW}참고: 데이터 볼륨은 보존되었습니다.${NC}"
    else
        echo -e "${RED}❌ 서비스 종료 실패${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}취소되었습니다.${NC}"
    echo -e "${BLUE}프로덕션 서비스가 계속 실행됩니다.${NC}"
fi