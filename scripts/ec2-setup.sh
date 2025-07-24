#!/bin/bash

# EC2 초기 설정 스크립트
# 이 스크립트는 EC2 인스턴스에서 한 번만 실행하면 됩니다.

set -e  # 에러 발생 시 스크립트 중단

echo "===== EC2 초기 설정 시작 ====="

# 1. 시스템 업데이트
echo "1. 시스템 업데이트 중..."
sudo yum update -y

# 2. Docker 설치
echo "2. Docker 설치 중..."
if ! command -v docker &> /dev/null; then
    sudo yum install docker -y
    sudo service docker start
    sudo systemctl enable docker
    sudo usermod -a -G docker $USER
    echo "Docker 설치 완료. 그룹 권한 적용을 위해 재로그인이 필요할 수 있습니다."
else
    echo "Docker가 이미 설치되어 있습니다."
fi

# 3. Docker Compose 설치
echo "3. Docker Compose 설치 중..."
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose 설치 완료"
else
    echo "Docker Compose가 이미 설치되어 있습니다."
fi

# 4. AWS CLI 설치 (ECR 로그인용)
echo "4. AWS CLI 설치 중..."
if ! command -v aws &> /dev/null; then
    sudo yum install aws-cli -y
    echo "AWS CLI 설치 완료"
else
    echo "AWS CLI가 이미 설치되어 있습니다."
fi

# 5. 디렉토리 구조 생성
echo "5. 필요한 디렉토리 생성 중..."
# 프로젝트 디렉토리는 GitHub Actions의 PROJECT_DIRECTORY 환경변수와 일치해야 함
PROJECT_DIR="${PROJECT_DIRECTORY:-techwikiplus-server}"
mkdir -p "$HOME/$PROJECT_DIR"

# 6. .env 파일 생성
echo "6. .env 파일 설정 중..."
if [ ! -f "$HOME/$PROJECT_DIR/.env" ]; then
    cat > "$HOME/$PROJECT_DIR/.env" << 'EOF'
# User Service 이미지 (CI/CD에서 자동 업데이트됨)
# 형식: <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/<ECR_REPOSITORY_NAME>:latest
# 예시: 127994096408.dkr.ecr.ap-northeast-2.amazonaws.com/techwikiplus/user-service:latest
USER_SERVICE_IMAGE=<ECR_REGISTRY>/<ECR_REPOSITORY_NAME>:latest

# 데이터베이스 설정
MYSQL_ROOT_PASSWORD=your-strong-password
MYSQL_DATABASE=techwikiplus
MYSQL_USER=techwikiplus
MYSQL_PASSWORD=your-strong-password
MYSQL_PORT=3306

# Redis 설정
REDIS_PASSWORD=your-strong-password
REDIS_PORT=6379

# JWT 설정
JWT_SECRET=your-64-character-secret-key
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# 메일 설정
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# 서비스 포트
USER_SERVICE_PORT=9000

# JPA 설정
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# 로깅 레벨
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_TECHWIKIPLUS=INFO
LOGGING_LEVEL_SPRING_WEB=WARN
LOGGING_LEVEL_SPRING_SECURITY=WARN
LOGGING_LEVEL_HIBERNATE_SQL=WARN

# JVM 옵션
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
EOF
    chmod 600 "$HOME/$PROJECT_DIR/.env"
    echo ".env 파일이 생성되었습니다. 실제 값으로 수정해주세요!"
    echo "편집하려면: nano ~/$PROJECT_DIR/.env"
else
    echo ".env 파일이 이미 존재합니다."
fi

# 7. 설치 완료 확인
echo ""
echo "===== 설치 완료 ====="
echo "설치된 버전:"
echo "- Docker: $(docker --version 2>/dev/null || echo '설치 실패')"
echo "- Docker Compose: $(docker-compose --version 2>/dev/null || echo '설치 실패')"
echo "- AWS CLI: $(aws --version 2>/dev/null || echo '설치 실패')"
echo ""
echo "===== 다음 단계 ====="
echo "1. .env 파일을 실제 값으로 수정하세요: nano ~/$PROJECT_DIR/.env"
echo "2. EC2 인스턴스에 IAM 역할 할당 (AmazonEC2ContainerRegistryReadOnly)"
echo "3. 보안 그룹에서 포트 9000 열기"
echo "4. Docker 그룹 권한 적용을 위해 재로그인: exit 후 다시 ssh 접속"
echo ""
echo "모든 설정이 완료되면 GitHub Actions가 자동으로 배포를 수행합니다."
echo "참고: 프로젝트 디렉토리는 ~/$PROJECT_DIR 입니다."