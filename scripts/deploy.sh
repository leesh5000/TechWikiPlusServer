#!/bin/bash

# 배포 스크립트
# GitHub Actions에서 호출되어 EC2에서 실행됩니다.

set -e  # 에러 발생 시 스크립트 중단

echo "===== 배포 시작 ====="
echo "시간: $(date)"
echo "디렉토리: $(pwd)"

# 1. 작업 디렉토리 확인
# PROJECT_DIRECTORY 환경변수 사용 (기본값: techwikiplus-server)
PROJECT_DIR="${PROJECT_DIRECTORY:-techwikiplus-server}"
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

# 5. ECR 로그인 (IAM 역할 사용)
echo "ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $(echo $IMAGE_TAG | cut -d'/' -f1) || {
    echo "WARNING: ECR 로그인 실패. IAM 역할이 설정되어 있는지 확인하세요."
}

# 6. 기존 컨테이너 정지 및 제거
echo "기존 컨테이너 정지 중..."
docker-compose down || true

# 7. 오래된 이미지 정리 (선택사항)
echo "오래된 이미지 정리 중..."
docker image prune -af --filter "until=24h" || true

# 8. 새 이미지로 컨테이너 시작
echo "새 컨테이너 시작 중..."
docker-compose up -d

# 9. 서비스 시작 대기
echo "서비스 시작 대기 중..."
sleep 30

# 10. 서비스 상태 확인
echo "===== 서비스 상태 ====="
docker-compose ps

# 11. 로그 확인 (마지막 50줄)
echo "===== 최근 로그 ====="
docker-compose logs --tail=50 user-service

# 12. 헬스체크
echo "===== 헬스체크 ====="
if curl -f http://localhost:9000/actuator/health 2>/dev/null; then
    echo ""
    echo "✅ 헬스체크 성공!"
else
    echo "⚠️  헬스체크 엔드포인트가 없거나 아직 준비되지 않았습니다."
fi

# 13. 배포 완료
echo ""
echo "===== 배포 완료 ====="
echo "시간: $(date)"
echo "이미지: ${IMAGE_TAG:-기존 이미지 사용}"

# 14. 실행 중인 컨테이너 정보
echo ""
echo "===== 실행 중인 컨테이너 ====="
docker ps --filter "name=techwikiplus"