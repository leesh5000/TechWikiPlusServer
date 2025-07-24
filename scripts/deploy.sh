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
HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://localhost:9000}"
HEALTH_CHECK_MAX_RETRIES="${HEALTH_CHECK_MAX_RETRIES:-10}"
HEALTH_CHECK_RETRY_DELAY="${HEALTH_CHECK_RETRY_DELAY:-5}"

cd ~/$PROJECT_DIR || { echo "$PROJECT_DIR 디렉토리가 없습니다. ec2-setup.sh를 먼저 실행하세요."; exit 1; }

# 2. .env 파일 확인
if [ ! -f ".env" ]; then
    echo "ERROR: .env 파일이 없습니다. ec2-setup.sh를 실행하고 .env 파일을 설정하세요."
    exit 1
fi

# 3. 현재 실행 중인 이미지 태그 저장 (롤백용)
echo "현재 실행 중인 이미지 정보 저장 중..."
CURRENT_IMAGE_TAG=$(docker inspect user-service 2>/dev/null | grep -o '"Image": "[^"]*"' | cut -d'"' -f4 || echo "none")
echo "현재 이미지: $CURRENT_IMAGE_TAG"

# 4. 새 이미지 태그로 환경변수 업데이트
if [ -n "$1" ]; then
    NEW_IMAGE_TAG="$1"
    echo "새 이미지 태그: $NEW_IMAGE_TAG"
    
    # 이미지가 실제로 변경되었는지 확인
    if [ "$CURRENT_IMAGE_TAG" = "$NEW_IMAGE_TAG" ]; then
        echo "이미지가 이미 최신 버전입니다. 배포를 건너뜁니다."
        exit 0
    fi
    
    # .env 파일의 USER_SERVICE_IMAGE 업데이트
    sed -i.bak "s|^USER_SERVICE_IMAGE=.*|USER_SERVICE_IMAGE=$NEW_IMAGE_TAG|" .env
    echo "USER_SERVICE_IMAGE 업데이트 완료"
else
    echo "WARNING: 이미지 태그가 제공되지 않았습니다. 기존 이미지를 사용합니다."
fi

# 5. docker-compose.yml 파일 확인
if [ ! -f "docker-compose.yml" ]; then
    echo "ERROR: docker-compose.yml 파일이 없습니다."
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

# docker-compose config로 현재 설정된 이미지 확인
for service in user-service mysql redis; do
    CONFIGURED_IMAGE=$(docker-compose config | grep -A 5 "^  $service:" | grep "image:" | awk '{print $2}')
    RUNNING_IMAGE=$(docker inspect ${PROJECT_DIR}_${service}_1 2>/dev/null | grep -o '"Image": "[^"]*"' | cut -d'"' -f4 || echo "none")
    
    if [ "$CONFIGURED_IMAGE" != "$RUNNING_IMAGE" ]; then
        echo "✓ $service: 업데이트 필요"
        echo "  현재: $RUNNING_IMAGE"
        echo "  신규: $CONFIGURED_IMAGE"
        SERVICES_TO_UPDATE="$SERVICES_TO_UPDATE $service"
    else
        echo "- $service: 이미 최신 버전"
    fi
done

# 8. 선택적 이미지 Pull
if [ -n "$SERVICES_TO_UPDATE" ]; then
    echo ""
    echo "===== 변경된 이미지만 Pull ====="
    
    # 각 서비스별로 pull (병렬 처리)
    for service in $SERVICES_TO_UPDATE; do
        echo "Pulling $service..."
        docker-compose pull --ignore-pull-failures $service &
    done
    
    # 모든 pull 작업 대기
    wait
    
    # Pull 결과 확인
    for service in $SERVICES_TO_UPDATE; do
        if docker-compose config | grep -A 5 "^  $service:" | grep "image:" | awk '{print $2}' | xargs docker inspect >/dev/null 2>&1; then
            echo "✅ $service: Pull 성공"
        else
            echo "❌ $service: Pull 실패"
            exit 1
        fi
    done
else
    echo "모든 서비스가 이미 최신 버전입니다."
fi

# 9. Rolling Update (무중단 배포)
if [ -n "$SERVICES_TO_UPDATE" ]; then
    echo ""
    echo "===== Rolling Update 시작 ====="
    
    # MySQL과 Redis는 먼저 업데이트 (의존성 때문에)
    for service in mysql redis; do
        if echo "$SERVICES_TO_UPDATE" | grep -q "$service"; then
            echo "Updating $service..."
            docker-compose up -d --no-deps $service
            sleep 5  # 데이터베이스 초기화 대기
        fi
    done
    
    # User Service 업데이트
    if echo "$SERVICES_TO_UPDATE" | grep -q "user-service"; then
        echo "Updating user-service..."
        
        # 새 컨테이너 시작 (기존 컨테이너는 유지)
        docker-compose up -d --no-deps --scale user-service=2 user-service
        
        # 새 컨테이너가 준비될 때까지 대기
        echo "새 컨테이너 초기화 대기 중 (30초)..."
        sleep 30
        
        # 헬스체크
        echo "헬스체크 수행 중..."
        RETRY_COUNT=0
        HEALTH_CHECK_PASSED=false
        
        while [ $RETRY_COUNT -lt $HEALTH_CHECK_MAX_RETRIES ]; do
            if curl -s -f $HEALTH_CHECK_URL > /dev/null 2>&1; then
                echo "✅ 헬스체크 성공!"
                HEALTH_CHECK_PASSED=true
                break
            else
                echo "헬스체크 재시도 중... ($((RETRY_COUNT+1))/$HEALTH_CHECK_MAX_RETRIES)"
                sleep $HEALTH_CHECK_RETRY_DELAY
                RETRY_COUNT=$((RETRY_COUNT+1))
            fi
        done
        
        if [ "$HEALTH_CHECK_PASSED" = true ]; then
            # 이전 컨테이너 제거
            echo "이전 컨테이너 제거 중..."
            docker-compose up -d --no-deps --scale user-service=1 user-service
        else
            echo "❌ 헬스체크 실패! 롤백 중..."
            # 롤백: 이전 이미지로 복원
            sed -i.bak "s|^USER_SERVICE_IMAGE=.*|USER_SERVICE_IMAGE=$CURRENT_IMAGE_TAG|" .env
            docker-compose up -d --no-deps user-service
            exit 1
        fi
    fi
else
    echo "업데이트할 서비스가 없습니다."
fi

# 10. 서비스 상태 확인
echo ""
echo "===== 서비스 상태 ====="
docker-compose ps

# 11. 로그 확인 (마지막 30줄)
echo ""
echo "===== 최근 로그 ====="
docker-compose logs --tail=30 user-service || {
    echo "WARNING: user-service 로그를 가져올 수 없습니다."
}

# 12. 스마트 이미지 정리
echo ""
echo "===== 이미지 정리 ====="

# 현재 사용 중인 이미지 ID 목록
USED_IMAGES=$(docker-compose ps -q | xargs docker inspect -f '{{.Image}}' 2>/dev/null | sort -u)

# 롤백용 이전 이미지도 보존
if [ -n "$CURRENT_IMAGE_TAG" ] && [ "$CURRENT_IMAGE_TAG" != "none" ]; then
    ROLLBACK_IMAGE_ID=$(docker images -q $CURRENT_IMAGE_TAG 2>/dev/null || echo "")
    if [ -n "$ROLLBACK_IMAGE_ID" ]; then
        USED_IMAGES="$USED_IMAGES $ROLLBACK_IMAGE_ID"
    fi
fi

# 사용하지 않는 이미지 삭제
echo "사용하지 않는 이미지 정리 중..."
DELETED_COUNT=0

# techwikiplus 관련 이미지만 대상으로
for image in $(docker images | grep -E "(techwikiplus|user-service)" | awk '{print $3}' | sort -u); do
    if ! echo "$USED_IMAGES" | grep -q "$image"; then
        echo "삭제: $(docker images | grep $image | head -1 | awk '{print $1":"$2}')"
        docker rmi $image 2>/dev/null && DELETED_COUNT=$((DELETED_COUNT+1)) || true
    fi
done

# dangling 이미지 정리
docker image prune -f > /dev/null 2>&1

echo "✅ $DELETED_COUNT개의 이미지를 정리했습니다."

# 13. 배포 완료
echo ""
echo "===== 배포 완료 ====="
echo "시간: $(date)"
echo "배포된 이미지: ${NEW_IMAGE_TAG:-기존 이미지 사용}"

# 14. 실행 중인 컨테이너 확인
echo ""
echo "===== 실행 중인 컨테이너 ====="
docker ps --filter "name=techwikiplus" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"

# 실제로 컨테이너가 실행 중인지 확인
RUNNING_CONTAINERS=$(docker-compose ps -q | wc -l)
EXPECTED_CONTAINERS=3  # mysql, redis, user-service

if [ "$RUNNING_CONTAINERS" -eq "$EXPECTED_CONTAINERS" ]; then
    echo ""
    echo "✅ 모든 서비스가 정상적으로 실행 중입니다. ($RUNNING_CONTAINERS/$EXPECTED_CONTAINERS)"
else
    echo ""
    echo "⚠️  WARNING: 일부 서비스가 실행되지 않았습니다. ($RUNNING_CONTAINERS/$EXPECTED_CONTAINERS)"
    docker-compose ps
fi

# 각 서비스 상태 확인
echo ""
echo "===== 서비스별 상태 확인 ====="
for service in mysql redis user-service; do
    if docker-compose ps | grep -q "$service.*Up"; then
        echo "✅ $service: 정상 실행 중"
    else
        echo "❌ $service: 실행되지 않음"
        echo "  로그 확인: docker-compose logs --tail=20 $service"
    fi
done

# 15. 배포 통계
echo ""
echo "===== 배포 통계 ====="
echo "총 소요 시간: $SECONDS초"
echo "업데이트된 서비스: ${SERVICES_TO_UPDATE:-없음}"
echo "정리된 이미지: $DELETED_COUNT개"