#!/bin/bash

# 배포 스크립트 - 빠른 배포 및 스마트 이미지 관리
# GitHub Actions에서 호출되어 EC2에서 실행됩니다.

set -e  # 에러 발생 시 스크립트 중단

echo "===== 배포 시작 ====="
echo "시간: $(date)"
echo "디렉토리: $(pwd)"
echo "사용자: $(whoami)"
echo "호스트: $(hostname)"

# 1. 작업 디렉토리 확인
# 환경변수 설정
PROJECT_DIR="${PROJECT_DIRECTORY:-techwikiplus-server}"
PROJECT_NAME="techwikiplus-server"
COMPOSE_BASE_FILE="docker/compose/docker-compose.base.yml"
COMPOSE_PROD_FILE="docker/compose/docker-compose.prod.yml"
ENV_FILE=".env.prod"
HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://localhost:9000/actuator/health}"
HEALTH_CHECK_MAX_RETRIES="${HEALTH_CHECK_MAX_RETRIES:-10}"
HEALTH_CHECK_RETRY_DELAY="${HEALTH_CHECK_RETRY_DELAY:-5}"

cd ~/$PROJECT_DIR || { echo "$PROJECT_DIR 디렉토리가 없습니다. ec2-setup.sh를 먼저 실행하세요."; exit 1; }

# 2. .env 파일 확인
if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: $ENV_FILE 파일이 없습니다. ec2-setup.sh를 실행하고 $ENV_FILE 파일을 설정하세요."
    exit 1
fi

# 3. 현재 실행 중인 이미지 태그 저장 (롤백용)
echo "현재 실행 중인 이미지 정보 저장 중..."
# 실제 컨테이너 이름은 docker-compose 프로젝트 이름을 포함
CONTAINER_NAME=$(docker ps --filter "label=com.docker.compose.service=user-service" --format "{{.Names}}" | head -1)
if [ -n "$CONTAINER_NAME" ]; then
    CURRENT_IMAGE_TAG=$(docker inspect $CONTAINER_NAME 2>/dev/null | grep -o '"Image": "[^"]*"' | cut -d'"' -f4 || echo "none")
else
    CURRENT_IMAGE_TAG="none"
fi
echo "현재 이미지: $CURRENT_IMAGE_TAG"

# 4. 새 이미지 태그로 환경변수 업데이트
if [ -n "$1" ]; then
    NEW_IMAGE_TAG="$1"
    echo "새 이미지 태그: $NEW_IMAGE_TAG"

    # 디버깅: 태그 형식 확인
    echo "태그 형식 분석:"
    echo "  - 전체 태그: $NEW_IMAGE_TAG"
    echo "  - 레지스트리: $(echo $NEW_IMAGE_TAG | cut -d'/' -f1)"
    echo "  - 리포지토리: $(echo $NEW_IMAGE_TAG | cut -d':' -f1 | cut -d'/' -f2-)"
    echo "  - 태그: $(echo $NEW_IMAGE_TAG | cut -d':' -f2)"

    # 이미지가 실제로 변경되었는지 확인
    if [ "$CURRENT_IMAGE_TAG" = "$NEW_IMAGE_TAG" ]; then
        echo "이미지가 이미 최신 버전입니다. 배포를 건너뜁니다."
        exit 0
    fi

    # .env 파일의 USER_SERVICE_IMAGE 업데이트
    echo "기존 USER_SERVICE_IMAGE: $(grep ^USER_SERVICE_IMAGE= $ENV_FILE || echo '없음')"
    sed -i.bak "s|^USER_SERVICE_IMAGE=.*|USER_SERVICE_IMAGE=$NEW_IMAGE_TAG|" $ENV_FILE
    echo "새로운 USER_SERVICE_IMAGE: $(grep ^USER_SERVICE_IMAGE= $ENV_FILE)"
    echo "USER_SERVICE_IMAGE 업데이트 완료"
else
    echo "WARNING: 이미지 태그가 제공되지 않았습니다. 기존 이미지를 사용합니다."
fi

# 5. docker-compose 파일 확인
if [ ! -f "$COMPOSE_BASE_FILE" ] || [ ! -f "$COMPOSE_PROD_FILE" ]; then
    echo "ERROR: docker-compose 파일이 없습니다."
    echo "필요한 파일: $COMPOSE_BASE_FILE, $COMPOSE_PROD_FILE"
    exit 1
fi

# 시스템 상태 확인
echo ""
echo "===== 시스템 상태 확인 ====="
echo "Docker 버전: $(docker --version)"
echo "Docker Compose 버전: $(docker-compose --version)"
echo "디스크 사용량:"
df -h | grep -E '^/dev/' | head -5
echo "메모리 사용량:"
free -h

# 6. ECR 로그인 (IAM 역할 사용)
echo "ECR 로그인 중..."
if [ -n "$NEW_IMAGE_TAG" ]; then
    ECR_REGISTRY=$(echo $NEW_IMAGE_TAG | cut -d'/' -f1)
    aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY || {
        echo "WARNING: ECR 로그인 실패. EC2 IAM 역할이 설정되어 있는지 확인하세요."
        echo "IAM 역할에 AmazonEC2ContainerRegistryReadOnly 권한이 필요합니다."
    }
fi

# 7. 변경된 서비스 확인
echo ""
echo "===== 변경된 서비스 확인 ====="
SERVICES_TO_UPDATE=""

# 각 서비스별로 이미지 변경 확인
for service in user-service mysql redis; do
    echo "서비스 확인 중: $service"

    # 설정된 이미지 (docker-compose config로 확인)
    CONFIGURED_IMAGE=$(docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE config 2>/dev/null | awk -v svc="$service" '/^services:/{in_services=1} in_services && $0 ~ "^  " svc ":"{in_service=1} in_service && /^    image:/{gsub(/^[ \t]+image:[ \t]*/, ""); print; exit}' || echo "error")

    # 실행 중인 이미지 확인
    RUNNING_CONTAINER=$(docker ps --filter "label=com.docker.compose.service=$service" --format "{{.Names}}" | head -1)
    if [ -n "$RUNNING_CONTAINER" ]; then
        # 컨테이너의 이미지 태그 확인 (Config.Image 사용)
        RUNNING_IMAGE=$(docker inspect $RUNNING_CONTAINER 2>/dev/null | jq -r '.[0].Config.Image' || echo "none")
    else
        RUNNING_IMAGE="none"
        echo "  컨테이너가 실행되지 않음"
    fi

    echo "  설정된 이미지: $CONFIGURED_IMAGE"
    echo "  실행 중인 이미지: $RUNNING_IMAGE"

    # 이미지가 다르거나 컨테이너가 없으면 업데이트 필요
    if [ "$CONFIGURED_IMAGE" != "$RUNNING_IMAGE" ] && [ "$CONFIGURED_IMAGE" != "error" ]; then
        echo "  ✓ 업데이트 필요"
        SERVICES_TO_UPDATE="$SERVICES_TO_UPDATE $service"
    else
        echo "  - 이미 최신 버전"
    fi
done

# 8. 이미지 Pull (항상 최신 버전 확인)
echo ""
echo "===== 이미지 Pull (최신 버전 확인) ====="

# 강제로 최신 이미지 pull (pull_policy: always와 함께 작동)
if [ -n "$NEW_IMAGE_TAG" ]; then
    echo "새 이미지 태그가 지정되었습니다. 모든 서비스의 최신 이미지를 확인합니다."

    # user-service는 항상 pull 시도 (최신 버전 확인)
    echo "user-service 최신 이미지 확인 중..."
    RETRY_COUNT=0
    MAX_RETRIES=3
    PULL_SUCCESS=false

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE pull user-service; then
            echo "✅ user-service: 최신 이미지 pull 성공"
            PULL_SUCCESS=true
            break
        else
            RETRY_COUNT=$((RETRY_COUNT+1))
            echo "❌ user-service: Pull 실패 (시도 $RETRY_COUNT/$MAX_RETRIES)"
            if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
                echo "10초 후 재시도..."
                sleep 10
            fi
        fi
    done

    if [ "$PULL_SUCCESS" = false ]; then
        echo "❌ user-service 이미지 pull 최종 실패"
        exit 1
    fi

    # 다른 서비스들도 변경사항이 있으면 pull
    if [ -n "$SERVICES_TO_UPDATE" ]; then
        for service in mysql redis; do
            if echo "$SERVICES_TO_UPDATE" | grep -q "$service"; then
                echo "$service 이미지 pull 중..."
                docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE pull $service || {
                    echo "❌ $service: Pull 실패"
                    exit 1
                }
                echo "✅ $service: Pull 성공"
            fi
        done
    fi
else
    echo "이미지 태그가 지정되지 않았습니다. 기존 이미지를 사용합니다."
fi

# 9. Rolling Update (무중단 배포)
if [ -n "$SERVICES_TO_UPDATE" ]; then
    echo ""
    echo "===== Rolling Update 시작 ====="

    # MySQL과 Redis는 먼저 업데이트 (의존성 때문에)
    for service in mysql redis; do
        if echo "$SERVICES_TO_UPDATE" | grep -q "$service"; then
            echo "Updating $service..."
            # 기존 컨테이너 중지 및 제거
            docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE stop $service || true
            docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE rm -f $service || true
            # 새 컨테이너 시작
            docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE up -d --no-deps $service
            sleep 5  # 데이터베이스 초기화 대기
        fi
    done

    # User Service 업데이트
    if echo "$SERVICES_TO_UPDATE" | grep -q "user-service"; then
        echo "Updating user-service..."

        # 기존 컨테이너를 down으로 완전히 제거
        echo "기존 user-service 컨테이너 완전 제거 중..."
        docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE down --remove-orphans --volumes user-service || true
        
        # 포트를 점유하는 프로세스 강제 종료
        echo "포트 9000을 점유하는 프로세스 확인 중..."
        PORT_PID=$(sudo lsof -ti:9000 2>/dev/null) || true
        if [ -n "$PORT_PID" ]; then
            echo "포트 9000을 점유하는 프로세스($PORT_PID) 강제 종료..."
            sudo kill -9 $PORT_PID 2>/dev/null || true
            sleep 2
        fi
        
        # Docker 네트워크 재생성
        echo "Docker 네트워크 정리 중..."
        docker network rm techwikiplus-server_techwikiplus-network 2>/dev/null || true
        
        # Dangling 리소스 정리
        echo "Docker dangling 리소스 정리 중..."
        docker system prune -f --volumes 2>/dev/null || true

        # 포트 해제 대기
        echo "포트 해제 대기 중..."
        PORT_RELEASED=false
        for i in {1..10}; do
            if ! sudo lsof -i:9000 >/dev/null 2>&1; then
                echo "포트 9000이 해제되었습니다."
                PORT_RELEASED=true
                break
            fi
            echo "대기 중... ($i/10)"
            # 재시도 중에도 포트를 점유하는 프로세스가 있으면 종료
            PORT_PID=$(sudo lsof -ti:9000 2>/dev/null) || true
            if [ -n "$PORT_PID" ]; then
                echo "  - 여전히 포트를 점유하는 프로세스($PORT_PID) 발견, 강제 종료..."
                sudo kill -9 $PORT_PID 2>/dev/null || true
            fi
            sleep 2
        done

        if [ "$PORT_RELEASED" = false ]; then
            echo "WARNING: 포트 9000이 여전히 사용 중입니다. 강제로 진행합니다."
        fi

        # 새 컨테이너 시작 (--force-recreate 옵션 추가)
        echo "새 user-service 컨테이너 시작 중..."
        COMPOSE_UP_RETRY=0
        COMPOSE_UP_SUCCESS=false
        
        while [ $COMPOSE_UP_RETRY -lt 3 ]; do
            if docker-compose -p techwikiplus-server -f $COMPOSE_BASE_FILE -f $COMPOSE_PROD_FILE --env-file $ENV_FILE up -d --force-recreate --no-deps user-service; then
                echo "✅ 컨테이너 시작 성공!"
                COMPOSE_UP_SUCCESS=true
                break
            else
                COMPOSE_UP_RETRY=$((COMPOSE_UP_RETRY+1))
                echo "❌ 컨테이너 시작 실패! (시도 $COMPOSE_UP_RETRY/3)"
                
                if [ $COMPOSE_UP_RETRY -lt 3 ]; then
                    echo "5초 후 재시도..."
                    sleep 5
                    
                    # 재시도 전 포트 재확인 및 정리
                    echo "포트 상태 재확인 중..."
                    PORT_PID=$(sudo lsof -ti:9000 2>/dev/null) || true
                    if [ -n "$PORT_PID" ]; then
                        echo "포트 9000을 여전히 점유하는 프로세스($PORT_PID) 강제 종료..."
                        sudo kill -9 $PORT_PID 2>/dev/null || true
                        sleep 2
                    fi
                    
                    # Docker 시스템 재정리
                    docker system prune -f 2>/dev/null || true
                fi
            fi
        done
        
        if [ "$COMPOSE_UP_SUCCESS" = false ]; then
            echo "❌ 컨테이너 시작 최종 실패!"
            exit 1
        fi

        # 컨테이너 시작 대기
        echo "컨테이너 초기화 대기 중 (30초)..."
        sleep 30

        # 헬스체크
        echo "헬스체크 수행 중..."
        echo "헬스체크 URL: $HEALTH_CHECK_URL"
        RETRY_COUNT=0
        HEALTH_CHECK_PASSED=false

        # 먼저 포트가 열려있는지 확인
        if sudo lsof -i:9000 >/dev/null 2>&1; then
            echo "포트 9000이 열려있습니다."
        else
            echo "WARNING: 포트 9000이 아직 열리지 않았습니다."
        fi

        while [ $RETRY_COUNT -lt $HEALTH_CHECK_MAX_RETRIES ]; do
            # curl 결과를 변수에 저장하여 디버그
            HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_CHECK_URL 2>&1)
            CURL_EXIT=$?

            if [ $CURL_EXIT -eq 0 ] && [ "$HTTP_STATUS" = "200" -o "$HTTP_STATUS" = "204" ]; then
                echo "✅ 헬스체크 성공! (HTTP $HTTP_STATUS)"
                HEALTH_CHECK_PASSED=true
                break
            else
                echo "헬스체크 재시도 중... ($((RETRY_COUNT+1))/$HEALTH_CHECK_MAX_RETRIES) - HTTP Status: $HTTP_STATUS, Exit Code: $CURL_EXIT"

                # 첫 번째 실패 시 더 자세한 정보 출력
                if [ $RETRY_COUNT -eq 0 ]; then
                    echo "컨테이너 상태 확인:"
                    docker ps --filter "label=com.docker.compose.service=user-service" --format "table {{.Names}}\t{{.Status}}"
                fi

                sleep $HEALTH_CHECK_RETRY_DELAY
                RETRY_COUNT=$((RETRY_COUNT+1))
            fi
        done

        if [ "$HEALTH_CHECK_PASSED" = false ]; then
            echo "❌ 헬스체크 실패! 서비스가 정상적으로 시작되지 않았습니다."
            # 로그 확인
            echo "User Service 로그:"
            USER_SERVICE_CONTAINER=$(docker ps --filter "label=com.docker.compose.service=user-service" --format "{{.Names}}" | head -1)
            if [ -n "$USER_SERVICE_CONTAINER" ]; then
                docker logs --tail=50 $USER_SERVICE_CONTAINER 2>&1 || echo "로그 확인 실패"
            else
                echo "User Service 컨테이너를 찾을 수 없습니다."
            fi
        fi
    fi
else
    echo "업데이트할 서비스가 없습니다."
fi

# 10. 서비스 상태 확인
echo ""
echo "===== 서비스 상태 ====="
# 안전한 방식으로 상태 확인
docker ps --filter "label=com.docker.compose.project=techwikiplus-server" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" || echo "WARNING: 컨테이너 상태 확인 실패"

# 11. 로그 확인 (마지막 30줄)
echo ""
echo "===== 최근 로그 ====="
USER_SERVICE_CONTAINER=$(docker ps --filter "label=com.docker.compose.service=user-service" --format "{{.Names}}" | head -1)
if [ -n "$USER_SERVICE_CONTAINER" ]; then
    docker logs --tail=30 $USER_SERVICE_CONTAINER 2>&1 || echo "WARNING: 로그 확인 실패"
else
    echo "WARNING: user-service 컨테이너를 찾을 수 없습니다."
fi

# 12. 스마트 이미지 정리
echo ""
echo "===== 이미지 정리 ====="

# 현재 사용 중인 이미지 ID 목록
USED_IMAGES=$(docker ps -a --format "{{.Image}}" | sort -u)

# 사용하지 않는 이미지 삭제
echo "사용하지 않는 이미지 정리 중..."
DELETED_COUNT=0

# dangling 이미지 정리
docker image prune -f > /dev/null 2>&1 && echo "Dangling 이미지 정리 완료"

# 오래된 techwikiplus 이미지 정리 (최신 2개만 유지)
for repo in $(docker images --format "{{.Repository}}" | grep -E "(techwikiplus|user-service)" | sort -u); do
    echo "저장소 확인: $repo"
    # 최신 2개를 제외한 이미지 삭제
    docker images $repo --format "{{.ID}} {{.CreatedAt}}" | sort -k2 -r | tail -n +3 | awk '{print $1}' | while read image_id; do
        if ! echo "$USED_IMAGES" | grep -q "$image_id"; then
            docker rmi $image_id 2>/dev/null && DELETED_COUNT=$((DELETED_COUNT+1)) && echo "  삭제: $image_id" || true
        fi
    done
done

echo "✅ $DELETED_COUNT개의 이미지를 정리했습니다."

# 13. 배포 완료
echo ""
echo "===== 배포 완료 ====="
echo "시간: $(date)"
echo "배포된 이미지: ${NEW_IMAGE_TAG:-기존 이미지 사용}"

# 14. 실행 중인 컨테이너 확인
echo ""
echo "===== 실행 중인 컨테이너 ====="

# 디버깅: 모든 컨테이너의 라벨 확인
echo "디버깅: 실행 중인 모든 컨테이너 라벨 확인"
docker ps --format "table {{.Names}}\t{{.Labels}}" | grep -E "(mysql|redis|user-service)" || echo "관련 컨테이너 없음"

# 디버깅: docker-compose 프로젝트로 필터링
echo ""
echo "디버깅: docker-compose 프로젝트 필터링 테스트"
echo "프로젝트 이름: techwikiplus-server"
docker ps --filter "label=com.docker.compose.project=techwikiplus-server" --format "table {{.Names}}\t{{.Status}}" || echo "필터링 결과 없음"

RUNNING_CONTAINERS=$(docker ps --filter "label=com.docker.compose.project=techwikiplus-server" -q | wc -l)
EXPECTED_CONTAINERS=3  # mysql, redis, user-service

echo ""
echo "실행 중인 컨테이너: $RUNNING_CONTAINERS/$EXPECTED_CONTAINERS"

# 각 서비스 상태 확인
echo ""
echo "===== 서비스별 상태 확인 ====="
for service in mysql redis user-service; do
    if docker ps --filter "label=com.docker.compose.service=$service" -q | grep -q .; then
        echo "✅ $service: 정상 실행 중"
    else
        echo "❌ $service: 실행되지 않음"
    fi
done

# 15. 배포 통계
echo ""
echo "===== 배포 통계 ====="
echo "총 소요 시간: $SECONDS초"
echo "업데이트된 서비스: ${SERVICES_TO_UPDATE:-없음}"
echo "정리된 이미지: $DELETED_COUNT개"

echo ""
echo "배포 프로세스가 완료되었습니다."
