#!/bin/bash

# Docker 리소스 정리 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${YELLOW}🧹 Docker 리소스 정리${NC}"

# 디렉토리 설정
setup_directories

# 정리 옵션 선택
echo "정리할 항목을 선택하세요:"
echo "1) 컨테이너만 중지 및 제거"
echo "2) 컨테이너 + 볼륨 제거 (데이터 삭제)"
echo "3) 컨테이너 + 이미지 제거"
echo "4) 전체 정리 (컨테이너 + 볼륨 + 이미지)"
echo "5) 시스템 전체 정리 (미사용 리소스 모두)"
echo "0) 취소"

read -p "선택 [0-5]: " choice

case $choice in
    1)
        echo -e "${BLUE}컨테이너 중지 및 제거 중...${NC}"
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down 2>/dev/null
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down 2>/dev/null
        docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down 2>/dev/null
        ;;
    2)
        echo -e "${YELLOW}⚠️  경고: 모든 데이터가 삭제됩니다!${NC}"
        read -p "계속하시겠습니까? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            echo -e "${BLUE}컨테이너 및 볼륨 제거 중...${NC}"
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down -v 2>/dev/null
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down -v 2>/dev/null
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v 2>/dev/null
        fi
        ;;
    3)
        echo -e "${BLUE}컨테이너 및 이미지 제거 중...${NC}"
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down --rmi all 2>/dev/null
        docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down --rmi all 2>/dev/null
        docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down --rmi all 2>/dev/null
        ;;
    4)
        echo -e "${YELLOW}⚠️  경고: 모든 데이터와 이미지가 삭제됩니다!${NC}"
        read -p "계속하시겠습니까? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            echo -e "${BLUE}전체 정리 중...${NC}"
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_LOCAL" down -v --rmi all 2>/dev/null
            docker-compose -p "$PROJECT_NAME_USER_SERVICE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD" down -v --rmi all 2>/dev/null
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v --rmi all 2>/dev/null
        fi
        ;;
    5)
        echo -e "${YELLOW}⚠️  경고: 시스템의 모든 미사용 Docker 리소스가 삭제됩니다!${NC}"
        read -p "계속하시겠습니까? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            echo -e "${BLUE}시스템 정리 중...${NC}"
            docker system prune -a --volumes -f
        fi
        ;;
    0)
        echo -e "${GREEN}취소되었습니다.${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}잘못된 선택입니다.${NC}"
        exit 1
        ;;
esac

echo -e "${GREEN}✅ 정리 완료!${NC}"

# 디스크 사용량 표시
echo -e "\n${BLUE}현재 Docker 디스크 사용량:${NC}"
docker system df