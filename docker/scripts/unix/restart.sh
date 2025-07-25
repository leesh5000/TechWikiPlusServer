#!/bin/bash

# Docker 서비스 재시작 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${BLUE}🔄 TechWikiPlus 서비스 재시작${NC}"

# 디렉토리 설정
setup_directories

# 환경 선택
echo "재시작할 환경을 선택하세요:"
echo "1) 로컬 개발 환경"
echo "2) 프로덕션 환경"
echo "0) 취소"

read -p "선택 [0-2]: " choice

case $choice in
    1)
        echo -e "${BLUE}로컬 개발 환경 재시작 중...${NC}"
        
        # 서비스별 재시작 옵션
        echo ""
        echo "재시작 옵션:"
        echo "1) 전체 서비스 재시작"
        echo "2) User Service만 재시작"
        echo "3) MySQL만 재시작"
        echo "4) Redis만 재시작"
        
        read -p "선택 [1-4]: " restart_option
        
        case $restart_option in
            1)
                docker-compose \
                    -p "$PROJECT_NAME_USER_SERVICE" \
                    -f "$COMPOSE_BASE" \
                    -f "$COMPOSE_LOCAL" \
                    restart
                ;;
            2)
                docker-compose \
                    -p "$PROJECT_NAME_USER_SERVICE" \
                    -f "$COMPOSE_BASE" \
                    -f "$COMPOSE_LOCAL" \
                    restart user-service
                ;;
            3)
                docker-compose \
                    -p "$PROJECT_NAME_USER_SERVICE" \
                    -f "$COMPOSE_BASE" \
                    -f "$COMPOSE_LOCAL" \
                    restart mysql
                ;;
            4)
                docker-compose \
                    -p "$PROJECT_NAME_USER_SERVICE" \
                    -f "$COMPOSE_BASE" \
                    -f "$COMPOSE_LOCAL" \
                    restart redis
                ;;
            *)
                echo -e "${RED}잘못된 선택입니다.${NC}"
                exit 1
                ;;
        esac
        ;;
    2)
        echo -e "${YELLOW}⚠️  경고: 프로덕션 서비스를 재시작하면 일시적으로 서비스가 중단됩니다!${NC}"
        read -p "계속하시겠습니까? (yes/N): " confirm
        
        if [[ $confirm == "yes" ]]; then
            echo -e "${BLUE}프로덕션 환경 재시작 중...${NC}"
            
            # Rolling restart 시뮬레이션
            echo -e "${BLUE}Rolling restart 수행 중...${NC}"
            
            # 먼저 새 컨테이너 시작
            docker-compose \
                -p "$PROJECT_NAME_USER_SERVICE" \
                -f "$COMPOSE_BASE" \
                -f "$COMPOSE_PROD" \
                up -d --no-deps user-service
            
            # 헬스체크 대기
            echo -e "${BLUE}헬스체크 대기 중 (${HEALTH_CHECK_WAIT}초)...${NC}"
            sleep "$HEALTH_CHECK_WAIT"
            
            # 이전 컨테이너 정리
            docker-compose \
                -p "$PROJECT_NAME_USER_SERVICE" \
                -f "$COMPOSE_BASE" \
                -f "$COMPOSE_PROD" \
                restart
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

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 서비스가 재시작되었습니다!${NC}"
    
    # 서비스 상태 확인
    echo -e "\n${BLUE}서비스 상태:${NC}"
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_LOCAL" \
        ps
else
    echo -e "${RED}❌ 재시작 실패${NC}"
    exit 1
fi