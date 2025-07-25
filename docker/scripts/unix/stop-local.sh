#!/bin/bash

# 로컬 개발 환경 종료 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${YELLOW}🛑 TechWikiPlus 로컬 개발 환경 종료${NC}"

# 디렉토리 설정
setup_directories

# 실행 중인 컨테이너 확인
echo -e "${BLUE}현재 실행 중인 서비스:${NC}"
docker-compose \
    -p "$PROJECT_NAME_USER_SERVICE" \
    -f "$COMPOSE_BASE" \
    -f "$COMPOSE_LOCAL" \
    ps

echo ""
read -p "서비스를 종료하시겠습니까? (y/N): " confirm

if [[ $confirm =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}서비스 종료 중...${NC}"
    
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_LOCAL" \
        down
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 모든 서비스가 종료되었습니다.${NC}"
        
        # 볼륨 제거 옵션
        echo ""
        read -p "데이터 볼륨도 제거하시겠습니까? (y/N): " remove_volumes
        
        if [[ $remove_volumes =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}⚠️  경고: 모든 데이터가 삭제됩니다!${NC}"
            read -p "정말로 계속하시겠습니까? (y/N): " confirm_volumes
            
            if [[ $confirm_volumes =~ ^[Yy]$ ]]; then
                echo -e "${BLUE}볼륨 제거 중...${NC}"
                docker-compose \
                    -p "$PROJECT_NAME_USER_SERVICE" \
                    -f "$COMPOSE_BASE" \
                    -f "$COMPOSE_LOCAL" \
                    down -v
                echo -e "${GREEN}✅ 볼륨이 제거되었습니다.${NC}"
            fi
        fi
    else
        echo -e "${RED}❌ 서비스 종료 실패${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}취소되었습니다.${NC}"
fi