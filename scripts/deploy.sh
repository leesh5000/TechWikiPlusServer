#!/bin/bash

# 배포 스크립트
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
HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://localhost:9000/actuator/health}"
HEALTH_CHECK_MAX_RETRIES="${HEALTH_CHECK_MAX_RETRIES:-10}"
HEALTH_CHECK_RETRY_DELAY="${HEALTH_CHECK_RETRY_DELAY:-5}"

cd ~/$PROJECT_DIR || { echo "$PROJECT_DIR 디렉토리가 없습니다. ec2-setup.sh를 먼저 실행하세요."; exit 1; }

# 2. .env 파일 확인
if [ ! -f ".env" ]; then
    echo "ERROR: .env 파일이 없습니다. ec2-setup.sh를 실행하고 .env 파일을 설정하세요."
    exit 1
fi

# 3. 새 이미지 태그로 환경변수 업데이트
if [ -n "$1" ]; then
    IMAGE_TAG="$1"
    echo "새 이미지 태그: $IMAGE_TAG"
    
    # .env 파일의 USER_SERVICE_IMAGE 업데이트
    sed -i.bak "s|^USER_SERVICE_IMAGE=.*|USER_SERVICE_IMAGE=$IMAGE_TAG|" .env
    echo "USER_SERVICE_IMAGE 업데이트 완료"
else
    echo "WARNING: 이미지 태그가 제공되지 않았습니다. 기존 이미지를 사용합니다."
fi

# 4. docker-compose.yml 파일 확인
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

# 5. ECR 로그인 (IAM 역할 사용)
echo "ECR 로그인 중..."
if [ -n "$IMAGE_TAG" ]; then
    ECR_REGISTRY=$(echo $IMAGE_TAG | cut -d'/' -f1)
    aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY || {
        echo "WARNING: ECR 로그인 실패. EC2 IAM 역할이 설정되어 있는지 확인하세요."
        echo "IAM 역할에 AmazonEC2ContainerRegistryReadOnly 권한이 필요합니다."
    }
fi

# 6. 기존 컨테이너 정지 및 제거
echo "기존 컨테이너 정지 중..."
docker-compose down || true

# 7. 오래된 이미지 정리 (선택사항)
echo "오래된 이미지 정리 중..."
docker image prune -af --filter "until=24h" || true

# 8. 최신 이미지 pull 및 컨테이너 시작
echo ""
echo "===== Docker 이미지 Pull ====="
echo "현재 환경변수:"
grep -E "^(USER_SERVICE_IMAGE|MYSQL_|REDIS_)" .env | head -5

echo ""
echo "최신 이미지 pull 중..."
docker-compose pull || {
    echo ""
    echo "❌ ERROR: Docker 이미지 pull 실패"
    echo "오류 원인:"
    echo "1. ECR 로그인 실패 - IAM 역할 확인"
    echo "2. 이미지 URL 오타 - .env 파일 확인"
    echo "3. 네트워크 문제 - 인터넷 연결 확인"
    echo ""
    echo "Docker 이미지 상태:"
    docker images | grep -E "(user-service|mysql|redis)" || echo "관련 이미지 없음"
    exit 1
}

echo ""
echo "===== Docker Compose 실행 ====="
echo "새 컨테이너 시작 중..."

# Docker Compose 실행 전 검증
docker-compose config > /dev/null 2>&1 || {
    echo "ERROR: docker-compose.yml 파일 검증 실패"
    docker-compose config
    exit 1
}

docker-compose up -d || {
    echo ""
    echo "❌ ERROR: Docker Compose 시작 실패"
    echo ""
    echo "===== 상세 오류 정보 ====="
    
    echo "\n1. Docker Compose 상태:"
    docker-compose ps -a
    
    echo "\n2. 각 서비스 로그:"
    echo "\n--- MySQL 로그 ---"
    docker-compose logs --tail=30 mysql || echo "MySQL 로그 없음"
    
    echo "\n--- Redis 로그 ---"
    docker-compose logs --tail=30 redis || echo "Redis 로그 없음"
    
    echo "\n--- User Service 로그 ---"
    docker-compose logs --tail=50 user-service || echo "User Service 로그 없음"
    
    echo "\n3. Docker 이벤트 (최근 2분):"
    docker events --since 2m --until now || echo "Docker 이벤트 없음"
    
    echo "\n4. 가능한 오류 원인:"
    echo "- 포트 충돌: 이미 사용 중인 포트"
    echo "- 메모리 부족: 컨테이너 시작에 필요한 메모리 부족"
    echo "- 볼륨 문제: Docker 볼륨 마운트 실패"
    echo "- 환경변수 오류: .env 파일의 필수 환경변수 누락"
    echo "- 의존성 문제: depends_on으로 지정된 서비스 시작 실패"
    
    exit 1
}

# 9. 서비스 시작 대기
echo "서비스 시작 대기 중..."
sleep 30

# 10. 서비스 상태 확인
echo "===== 서비스 상태 ====="
docker-compose ps

# 11. 로그 확인 (마지막 50줄)
echo "===== 최근 로그 ====="
docker-compose logs --tail=50 user-service || {
    echo "WARNING: user-service 로그를 가져올 수 없습니다."
    echo "컨테이너 상태 확인:"
    docker ps -a | grep user-service
}

# 12. 헬스체크
echo "===== 헬스체크 ====="
echo "헬스체크 URL: $HEALTH_CHECK_URL"
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $HEALTH_CHECK_MAX_RETRIES ]; do
    if curl -f $HEALTH_CHECK_URL 2>/dev/null; then
        echo ""
        echo "✅ 헬스체크 성공!"
        break
    else
        echo "헬스체크 실패... 재시도 중 ($((RETRY_COUNT+1))/$HEALTH_CHECK_MAX_RETRIES)"
        sleep $HEALTH_CHECK_RETRY_DELAY
        RETRY_COUNT=$((RETRY_COUNT+1))
    fi
done

if [ $RETRY_COUNT -eq $HEALTH_CHECK_MAX_RETRIES ]; then
    echo ""
    echo "⚠️  WARNING: 헬스체크가 계속 실패합니다."
    echo "시도한 URL: $HEALTH_CHECK_URL"
    echo "서비스 로그를 확인하세요."
fi

# 13. 배포 완료
echo ""
echo "===== 배포 완료 ====="
echo "시간: $(date)"
echo "이미지: ${IMAGE_TAG:-기존 이미지 사용}"

# 14. 실행 중인 컨테이너 확인
echo ""
echo "===== 실행 중인 컨테이너 ====="
docker ps --filter "name=techwikiplus"

# 실제로 컨테이너가 실행 중인지 확인
RUNNING_CONTAINERS=$(docker-compose ps -q | wc -l)
EXPECTED_CONTAINERS=3  # mysql, redis, user-service

if [ "$RUNNING_CONTAINERS" -eq "0" ]; then
    echo ""
    echo "❌ ERROR: 실행 중인 컨테이너가 없습니다!"
    echo ""
    echo "===== 상세 진단 정보 ====="
    echo "1. 모든 컨테이너 상태:"
    docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo "\n2. Docker Compose 로그:"
    docker-compose logs --tail=100
    
    echo "\n3. Docker 네트워크:"
    docker network ls | grep techwikiplus
    
    echo "\n4. Docker 볼륨:"
    docker volume ls | grep techwikiplus
    
    exit 1
elif [ "$RUNNING_CONTAINERS" -lt "$EXPECTED_CONTAINERS" ]; then
    echo ""
    echo "⚠️  WARNING: 예상보다 적은 컨테이너가 실행 중입니다."
    echo "예상: $EXPECTED_CONTAINERS개, 실제: $RUNNING_CONTAINERS개"
    echo ""
    echo "실행 중인 컨테이너:"
    docker-compose ps
    echo ""
    echo "중지된 컨테이너:"
    docker-compose ps -a | grep -E "Exit|Created"
fi

echo ""
echo "✅ $RUNNING_CONTAINERS개의 컨테이너가 실행 중입니다."

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