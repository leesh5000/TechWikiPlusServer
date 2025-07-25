#!/bin/bash

# 인프라(MySQL, Redis) 종료 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

echo -e "${YELLOW}🛑 TechWikiPlus 인프라 서비스 종료${NC}"

# 디렉토리 설정
setup_directories

# 실행 중인 인프라 서비스 확인
echo -e "${BLUE}현재 실행 중인 인프라 서비스:${NC}"
docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" ps

# 실행 중인 서비스가 있는지 확인
RUNNING_SERVICES=$(docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" ps -q | wc -l)

if [ $RUNNING_SERVICES -eq 0 ]; then
    echo -e "\n${YELLOW}실행 중인 인프라 서비스가 없습니다.${NC}"
    exit 0
fi

echo ""
read -p "인프라 서비스를 종료하시겠습니까? (y/N): " confirm

if [[ $confirm =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}인프라 서비스 종료 중...${NC}"
    
    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 인프라 서비스가 종료되었습니다.${NC}"
        
        # 볼륨 제거 옵션
        echo ""
        echo -e "${YELLOW}⚠️  데이터 볼륨 관리${NC}"
        echo "1) 데이터 유지 (기본)"
        echo "2) MySQL 데이터만 삭제"
        echo "3) Redis 데이터만 삭제"
        echo "4) 모든 데이터 삭제"
        
        read -p "선택 [1-4] (기본: 1): " volume_choice
        
        case ${volume_choice:-1} in
            1)
                echo -e "${GREEN}데이터가 유지됩니다.${NC}"
                ;;
            2)
                echo -e "${YELLOW}⚠️  경고: MySQL 데이터가 삭제됩니다!${NC}"
                read -p "정말로 계속하시겠습니까? (y/N): " confirm_mysql
                
                if [[ $confirm_mysql =~ ^[Yy]$ ]]; then
                    docker volume rm techwikiplus_mysql-data 2>/dev/null || \
                    docker volume rm $(docker volume ls -q | grep mysql-data) 2>/dev/null
                    echo -e "${GREEN}✅ MySQL 데이터가 삭제되었습니다.${NC}"
                fi
                ;;
            3)
                echo -e "${YELLOW}⚠️  경고: Redis 데이터가 삭제됩니다!${NC}"
                read -p "정말로 계속하시겠습니까? (y/N): " confirm_redis
                
                if [[ $confirm_redis =~ ^[Yy]$ ]]; then
                    docker volume rm techwikiplus_redis-data 2>/dev/null || \
                    docker volume rm $(docker volume ls -q | grep redis-data) 2>/dev/null
                    echo -e "${GREEN}✅ Redis 데이터가 삭제되었습니다.${NC}"
                fi
                ;;
            4)
                echo -e "${YELLOW}⚠️  경고: 모든 데이터가 삭제됩니다!${NC}"
                read -p "정말로 계속하시겠습니까? (y/N): " confirm_all
                
                if [[ $confirm_all =~ ^[Yy]$ ]]; then
                    docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" down -v
                    echo -e "${GREEN}✅ 모든 데이터가 삭제되었습니다.${NC}"
                fi
                ;;
            *)
                echo -e "${GREEN}데이터가 유지됩니다.${NC}"
                ;;
        esac
        
        # 디스크 사용량 표시
        echo -e "\n${BLUE}Docker 볼륨 사용량:${NC}"
        docker volume ls | grep -E "(mysql|redis)" || echo "사용 중인 볼륨이 없습니다."
        
    else
        echo -e "${RED}❌ 인프라 서비스 종료 실패${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}취소되었습니다.${NC}"
fi