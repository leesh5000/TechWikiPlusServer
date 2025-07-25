#!/bin/bash

# TechWikiPlus Docker 통합 관리 스크립트

# 공통 설정 로드
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/common.sh"

# 디렉토리 및 BuildKit 설정
setup_directories
enable_buildkit

# 로고 출력
show_logo() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════╗"
    echo "║      TechWikiPlus Docker Manager 🐳        ║"
    echo "╚════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# 메인 메뉴
show_main_menu() {
    echo -e "${GREEN}메인 메뉴${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 🚀 서비스 시작"
    echo "2) 🛑 서비스 종료"
    echo "3) 🔄 서비스 재시작"
    echo "4) 📊 서비스 상태 확인"
    echo "5) 📋 로그 확인"
    echo "6) 🧹 Docker 리소스 정리"
    echo "7) 🔧 고급 옵션"
    echo "0) 종료"
    echo "─────────────────────────────────────────────"
}

# 서비스 시작 메뉴
start_services_menu() {
    echo -e "\n${GREEN}서비스 시작${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 로컬 개발 환경 (전체)"
    echo "2) 프로덕션 환경 (전체)"
    echo "3) 인프라만 (MySQL + Redis)"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    case $choice in
        1) start_local ;;
        2) start_production ;;
        3) start_infra ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
}

# 로컬 환경 시작
start_local() {
    echo -e "\n${BLUE}로컬 개발 환경을 시작합니다...${NC}"
    
    # .env.local 파일 확인
    check_env_file "$ENV_LOCAL" "$ENV_LOCAL_EXAMPLE"
    
    # 실행
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_LOCAL" \
        --env-file "$ENV_LOCAL" \
        up -d --build
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 로컬 환경이 시작되었습니다!${NC}"
        show_service_details
    else
        echo -e "${RED}❌ 시작 실패${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 프로덕션 환경 시작
start_production() {
    echo -e "\n${YELLOW}⚠️  프로덕션 환경을 시작합니다...${NC}"
    
    # .env.prod 파일 확인
    if [ ! -f "$ENV_PROD" ]; then
        echo -e "${RED}❌ $ENV_PROD 파일이 없습니다!${NC}"
        echo "먼저 환경 파일을 생성하세요:"
        echo "  cp $ENV_PROD_EXAMPLE $ENV_PROD"
        read -p "계속하려면 Enter를 누르세요..."
        return
    fi
    
    read -p "정말로 프로덕션을 시작하시겠습니까? (yes/N): " confirm
    if [[ $confirm != "yes" ]]; then
        return
    fi
    
    # 실행
    docker-compose \
        -p "$PROJECT_NAME_USER_SERVICE" \
        -f "$COMPOSE_BASE" \
        -f "$COMPOSE_PROD" \
        --env-file "$ENV_PROD" \
        up -d
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 프로덕션 환경이 시작되었습니다!${NC}"
        show_service_details
    else
        echo -e "${RED}❌ 시작 실패${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 인프라만 시작
start_infra() {
    echo -e "\n${BLUE}인프라 서비스를 시작합니다...${NC}"
    
    echo "환경을 선택하세요:"
    echo "1) 로컬 환경 설정"
    echo "2) 프로덕션 환경 설정"
    echo "3) 기본값 사용"
    
    read -p "선택: " env_choice
    
    case $env_choice in
        1)
            check_env_file "$ENV_LOCAL" "$ENV_LOCAL_EXAMPLE"
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" --env-file "$ENV_LOCAL" up -d
            ;;
        2)
            if [ ! -f "$ENV_PROD" ]; then
                echo -e "${RED}❌ $ENV_PROD 파일이 없습니다!${NC}"
                read -p "계속하려면 Enter를 누르세요..."
                return
            fi
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" --env-file "$ENV_PROD" up -d
            ;;
        3)
            docker-compose -p "$PROJECT_NAME_INFRA" -f "$COMPOSE_BASE" up -d
            ;;
        *)
            echo -e "${RED}잘못된 선택입니다.${NC}"
            return
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 인프라가 시작되었습니다!${NC}"
        echo -e "\n${CYAN}연결 정보:${NC}"
        echo "  MySQL: localhost:${MYSQL_PORT:-$MYSQL_PORT_DEFAULT}"
        echo "  Redis: localhost:${REDIS_PORT:-$REDIS_PORT_DEFAULT}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 서비스 종료 메뉴
stop_services_menu() {
    echo -e "\n${RED}서비스 종료${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 로컬 환경 종료"
    echo "2) 프로덕션 환경 종료"
    echo "3) 인프라만 종료"
    echo "4) 모든 서비스 종료"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    case $choice in
        1) stop_local ;;
        2) stop_production ;;
        3) stop_infra ;;
        4) stop_all ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
}

# 로컬 환경 종료
stop_local() {
    echo -e "\n${YELLOW}로컬 환경을 종료합니다...${NC}"
    
    docker-compose \
        -p techwikiplus-server-user-service \
        -f docker/compose/docker-compose.base.yml \
        -f docker/compose/docker-compose.local.yml \
        ps
    
    read -p "종료하시겠습니까? (y/N): " confirm
    if [[ $confirm =~ ^[Yy]$ ]]; then
        docker-compose \
            -p techwikiplus-server-user-service \
            -f docker/compose/docker-compose.base.yml \
            -f docker/compose/docker-compose.local.yml \
            down
        
        echo -e "${GREEN}✅ 로컬 환경이 종료되었습니다.${NC}"
        
        read -p "볼륨도 삭제하시겠습니까? (y/N): " del_volumes
        if [[ $del_volumes =~ ^[Yy]$ ]]; then
            docker-compose \
                -p techwikiplus-server-user-service \
                -f docker/compose/docker-compose.base.yml \
                -f docker/compose/docker-compose.local.yml \
                down -v
            echo -e "${GREEN}✅ 볼륨이 삭제되었습니다.${NC}"
        fi
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 프로덕션 환경 종료
stop_production() {
    echo -e "\n${RED}⚠️  프로덕션 환경을 종료합니다...${NC}"
    
    docker-compose \
        -p techwikiplus-server-user-service \
        -f docker/compose/docker-compose.base.yml \
        -f docker/compose/docker-compose.prod.yml \
        ps
    
    read -p "정말로 프로덕션을 종료하시겠습니까? (yes/N): " confirm
    if [[ $confirm == "yes" ]]; then
        echo -e "${BLUE}Graceful shutdown 진행 중...${NC}"
        docker-compose \
            -p techwikiplus-server-user-service \
            -f docker/compose/docker-compose.base.yml \
            -f docker/compose/docker-compose.prod.yml \
            stop -t 10
        
        docker-compose \
            -p techwikiplus-server-user-service \
            -f docker/compose/docker-compose.base.yml \
            -f docker/compose/docker-compose.prod.yml \
            down
        
        echo -e "${GREEN}✅ 프로덕션 환경이 종료되었습니다.${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 인프라만 종료
stop_infra() {
    echo -e "\n${YELLOW}인프라 서비스를 종료합니다...${NC}"
    
    docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml ps
    
    read -p "종료하시겠습니까? (y/N): " confirm
    if [[ $confirm =~ ^[Yy]$ ]]; then
        docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml down
        echo -e "${GREEN}✅ 인프라가 종료되었습니다.${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 모든 서비스 종료
stop_all() {
    echo -e "\n${RED}⚠️  모든 서비스를 종료합니다...${NC}"
    
    docker ps --filter "label=com.docker.compose.project" --format "table {{.Names}}\t{{.Status}}"
    
    read -p "모든 서비스를 종료하시겠습니까? (y/N): " confirm
    if [[ $confirm =~ ^[Yy]$ ]]; then
        # 로컬 환경
        docker-compose \
            -p techwikiplus-server-user-service \
            -f docker/compose/docker-compose.base.yml \
            -f docker/compose/docker-compose.local.yml \
            down 2>/dev/null
        
        # 프로덕션 환경
        docker-compose \
            -p techwikiplus-server-user-service \
            -f docker/compose/docker-compose.base.yml \
            -f docker/compose/docker-compose.prod.yml \
            down 2>/dev/null
        
        # 인프라만
        docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml down 2>/dev/null
        
        echo -e "${GREEN}✅ 모든 서비스가 종료되었습니다.${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 재시작 메뉴
restart_services_menu() {
    echo -e "\n${BLUE}서비스 재시작${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 전체 서비스 재시작"
    echo "2) User Service만 재시작"
    echo "3) MySQL만 재시작"
    echo "4) Redis만 재시작"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    # 어떤 환경인지 확인
    echo -e "\n${CYAN}환경 선택:${NC}"
    echo "1) 로컬 환경"
    echo "2) 프로덕션 환경"
    read -p "선택: " env_choice
    
    case $env_choice in
        1) PROJECT_NAME="techwikiplus-server-user-service"
           COMPOSE_FILES="-f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml" ;;
        2) PROJECT_NAME="techwikiplus-server-user-service"
           COMPOSE_FILES="-f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.prod.yml" ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}"; return ;;
    esac
    
    case $choice in
        1) docker-compose -p $PROJECT_NAME $COMPOSE_FILES restart ;;
        2) docker-compose -p $PROJECT_NAME $COMPOSE_FILES restart user-service ;;
        3) docker-compose -p $PROJECT_NAME $COMPOSE_FILES restart mysql ;;
        4) docker-compose -p $PROJECT_NAME $COMPOSE_FILES restart redis ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 재시작 완료${NC}"
    fi
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 상태 확인
check_status() {
    echo -e "\n${CYAN}서비스 상태${NC}"
    echo "─────────────────────────────────────────────"
    
    echo -e "\n${BLUE}실행 중인 컨테이너:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo -e "\n${BLUE}Docker 네트워크:${NC}"
    docker network ls | grep techwikiplus
    
    echo -e "\n${BLUE}Docker 볼륨:${NC}"
    docker volume ls | grep -E "(mysql|redis)"
    
    echo -e "\n${BLUE}리소스 사용량:${NC}"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 로그 확인 메뉴
view_logs_menu() {
    echo -e "\n${CYAN}로그 확인${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 모든 서비스 로그"
    echo "2) User Service 로그"
    echo "3) MySQL 로그"
    echo "4) Redis 로그"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    # 어떤 환경인지 확인
    echo -e "\n${CYAN}환경 선택:${NC}"
    echo "1) 로컬 환경"
    echo "2) 프로덕션 환경"
    echo "3) 인프라만"
    read -p "선택: " env_choice
    
    case $env_choice in
        1) PROJECT_NAME="techwikiplus-server-user-service"
           COMPOSE_FILES="-f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml" ;;
        2) PROJECT_NAME="techwikiplus-server-user-service"
           COMPOSE_FILES="-f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.prod.yml" ;;
        3) PROJECT_NAME="techwikiplus-server-infra"
           COMPOSE_FILES="-f docker/compose/docker-compose.base.yml" ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}"; return ;;
    esac
    
    case $choice in
        1) docker-compose -p $PROJECT_NAME $COMPOSE_FILES logs -f --tail=100 ;;
        2) docker-compose -p $PROJECT_NAME $COMPOSE_FILES logs -f --tail=100 user-service ;;
        3) docker-compose -p $PROJECT_NAME $COMPOSE_FILES logs -f --tail=100 mysql ;;
        4) docker-compose -p $PROJECT_NAME $COMPOSE_FILES logs -f --tail=100 redis ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
}

# Docker 리소스 정리
cleanup_menu() {
    echo -e "\n${YELLOW}Docker 리소스 정리${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 중지된 컨테이너 제거"
    echo "2) 사용하지 않는 이미지 제거"
    echo "3) 사용하지 않는 볼륨 제거"
    echo "4) 사용하지 않는 네트워크 제거"
    echo "5) 전체 시스템 정리 (주의!)"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    case $choice in
        1)
            docker container prune -f
            echo -e "${GREEN}✅ 중지된 컨테이너가 제거되었습니다.${NC}"
            ;;
        2)
            docker image prune -f
            echo -e "${GREEN}✅ 사용하지 않는 이미지가 제거되었습니다.${NC}"
            ;;
        3)
            read -p "정말로 볼륨을 제거하시겠습니까? 데이터가 삭제됩니다! (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                docker volume prune -f
                echo -e "${GREEN}✅ 사용하지 않는 볼륨이 제거되었습니다.${NC}"
            fi
            ;;
        4)
            docker network prune -f
            echo -e "${GREEN}✅ 사용하지 않는 네트워크가 제거되었습니다.${NC}"
            ;;
        5)
            echo -e "${RED}⚠️  경고: 모든 미사용 Docker 리소스가 삭제됩니다!${NC}"
            read -p "정말로 전체 정리를 하시겠습니까? (yes/N): " confirm
            if [[ $confirm == "yes" ]]; then
                docker system prune -a --volumes -f
                echo -e "${GREEN}✅ 시스템 정리가 완료되었습니다.${NC}"
            fi
            ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 고급 옵션 메뉴
advanced_menu() {
    echo -e "\n${MAGENTA}고급 옵션${NC}"
    echo "─────────────────────────────────────────────"
    echo "1) 컨테이너 쉘 접속"
    echo "2) MySQL 클라이언트 접속"
    echo "3) Redis CLI 접속"
    echo "4) Docker Compose 설정 검증"
    echo "5) 환경변수 파일 편집"
    echo "0) 메인 메뉴로"
    echo "─────────────────────────────────────────────"
    
    read -p "선택: " choice
    
    case $choice in
        1)
            echo -e "\n${CYAN}접속할 컨테이너:${NC}"
            docker ps --format "table {{.Names}}"
            read -p "컨테이너 이름: " container_name
            docker exec -it $container_name sh
            ;;
        2)
            read -sp "MySQL 비밀번호: " password
            echo
            docker exec -it "$CONTAINER_MYSQL" mysql -u "$MYSQL_USER_DEFAULT" -p$password
            ;;
        3)
            read -sp "Redis 비밀번호: " password
            echo
            docker exec -it "$CONTAINER_REDIS" redis-cli -a $password
            ;;
        4)
            echo -e "\n${CYAN}Docker Compose 설정 검증:${NC}"
            echo "1) 로컬 환경"
            echo "2) 프로덕션 환경"
            read -p "환경 선택: " env_choice
            case $env_choice in
                1) docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml config ;;
                2) docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.prod.yml config ;;
                *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
            esac
            ;;
        5)
            echo -e "\n${CYAN}편집할 환경변수 파일:${NC}"
            echo "1) .env.local"
            echo "2) .env.prod"
            read -p "선택: " env_choice
            case $env_choice in
                1) ${EDITOR:-vi} .env.local ;;
                2) ${EDITOR:-vi} .env.prod ;;
                *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
            esac
            ;;
        0) return ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
    
    read -p "계속하려면 Enter를 누르세요..."
}

# 서비스 정보 출력 래퍼 함수
show_service_details() {
    # common.sh에서 정의된 함수 호출
    show_service_info
    
    # 현재 사용 중인 환경 파일 확인
    if [ -f "$ENV_LOCAL" ]; then
        show_env_info "$ENV_LOCAL"
    elif [ -f "$ENV_PROD" ]; then
        show_env_info "$ENV_PROD"
    fi
}

# 메인 루프
main() {
    clear
    show_logo
    
    while true; do
        show_main_menu
        read -p "선택: " choice
        
        case $choice in
            1) start_services_menu ;;
            2) stop_services_menu ;;
            3) restart_services_menu ;;
            4) check_status ;;
            5) view_logs_menu ;;
            6) cleanup_menu ;;
            7) advanced_menu ;;
            0)
                echo -e "\n${GREEN}Docker Manager를 종료합니다. 안녕히 가세요!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}잘못된 선택입니다. 다시 선택해주세요.${NC}"
                read -p "계속하려면 Enter를 누르세요..."
                ;;
        esac
        
        clear
        show_logo
    done
}

# 스크립트 실행
main