# GitHub Secrets 설정 가이드

이 문서는 TechWikiPlus 프로젝트의 CI/CD 파이프라인에 필요한 GitHub Secrets 설정 방법을 안내합니다.

## 필요한 GitHub Secrets

### 1. AWS 관련 Secrets

#### `AWS_ACCESS_KEY_ID`
- **설명**: AWS 서비스에 접근하기 위한 액세스 키 ID
- **획득 방법**: AWS IAM 콘솔에서 사용자 생성 후 액세스 키 발급
- **필요 권한**:
  - ECR: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`

#### `AWS_SECRET_ACCESS_KEY`
- **설명**: AWS 액세스 키에 대응하는 시크릿 키
- **획득 방법**: 액세스 키 생성 시 함께 제공됨
- **주의사항**: 한 번만 표시되므로 안전하게 보관 필요

### 2. EC2 접속 관련 Secrets

#### `EC2_HOST`
- **설명**: 배포 대상 EC2 인스턴스의 퍼블릭 IP 또는 도메인
- **예시**: `52.79.xxx.xxx` 또는 `api.techwikiplus.com`

#### `EC2_USERNAME`
- **설명**: EC2 접속 시 사용할 사용자명
- **기본값**: Amazon Linux 2는 `ec2-user`, Ubuntu는 `ubuntu`

#### `EC2_SSH_KEY`
- **설명**: EC2 인스턴스 접속용 프라이빗 SSH 키
- **설정 방법**:
  ```bash
  # 로컬에서 프라이빗 키 내용 복사
  cat ~/.ssh/your-ec2-key.pem
  ```
- **주의사항**: 전체 내용을 복사 (-----BEGIN RSA PRIVATE KEY----- 포함)

### 3. 데이터베이스 관련 Secrets

#### `MYSQL_ROOT_PASSWORD`
- **설명**: MySQL root 사용자 비밀번호
- **권장사항**: 최소 16자 이상의 강력한 비밀번호 사용

#### `MYSQL_DATABASE`
- **설명**: 사용할 데이터베이스명
- **기본값**: `techwikiplus`

#### `MYSQL_USER`
- **설명**: 애플리케이션용 MySQL 사용자명
- **기본값**: `techwikiplus`

#### `MYSQL_PASSWORD`
- **설명**: 애플리케이션용 MySQL 사용자 비밀번호
- **권장사항**: root 비밀번호와 다른 강력한 비밀번호 사용

### 4. Redis 관련 Secrets

#### `REDIS_PASSWORD`
- **설명**: Redis 접속 비밀번호
- **권장사항**: 최소 16자 이상의 강력한 비밀번호 사용

### 5. JWT 관련 Secrets

#### `JWT_SECRET`
- **설명**: JWT 토큰 서명용 시크릿 키
- **요구사항**: 최소 32자 이상 (256비트)
- **생성 방법**:
  ```bash
  # Linux/Mac
  openssl rand -base64 64
  
  # 또는 온라인 생성기 사용 (프로덕션 비권장)
  ```

#### `JWT_ACCESS_TOKEN_EXPIRATION`
- **설명**: 액세스 토큰 유효 시간 (밀리초)
- **기본값**: `3600000` (1시간)

#### `JWT_REFRESH_TOKEN_EXPIRATION`
- **설명**: 리프레시 토큰 유효 시간 (밀리초)
- **기본값**: `604800000` (7일)

### 6. 메일 관련 Secrets

#### `MAIL_USERNAME`
- **설명**: SMTP 서버 접속용 이메일 주소
- **예시**: `noreply@techwikiplus.com`

#### `MAIL_PASSWORD`
- **설명**: SMTP 서버 접속용 비밀번호
- **Gmail 사용 시**: 앱 비밀번호 생성 필요 (2단계 인증 필수)

## ECR 리포지토리 설정

CI/CD 파이프라인에서 사용하는 ECR 리포지토리 이름은 `.github/workflows/ci-cd.yml` 파일의 상단에서 설정합니다:

```yaml
env:
  AWS_REGION: ap-northeast-2
  ECR_REPOSITORY_NAME: techwikiplus/user-service  # 프로젝트에 맞게 변경
  PROJECT_DIRECTORY: techwikiplus-server  # EC2에서 사용할 프로젝트 디렉토리
  HEALTH_CHECK_URL: http://localhost:9000/actuator/health  # 서비스 헬스체크 URL
```

다른 프로젝트에서 사용 시:
- `ECR_REPOSITORY_NAME`: 해당 프로젝트의 ECR 리포지토리 이름으로 변경
- `PROJECT_DIRECTORY`: EC2에서 사용할 디렉토리 이름으로 변경
- `HEALTH_CHECK_URL`: 서비스의 헬스체크 엔드포인트로 변경

## GitHub에서 Secrets 설정하기

### Repository Secrets 설정 (모든 환경 공통)
1. GitHub 리포지토리로 이동
2. Settings → Secrets and variables → Actions 클릭
3. "New repository secret" 버튼 클릭
4. Name과 Secret 입력 후 저장

### Environment Secrets 설정 (프로덕션 환경 전용)
1. GitHub 리포지토리로 이동
2. Settings → Environments 클릭
3. "New environment" 클릭하여 `production` 환경 생성
4. Environment secrets에서 "Add secret" 클릭
5. 프로덕션 전용 시크릿 추가

#### Production Environment에 설정할 Secrets:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_SSH_KEY`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION`
- `JWT_REFRESH_TOKEN_EXPIRATION`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `SLACK_WEBHOOK_URL` (선택사항)

**주의**: Environment Secrets는 해당 environment를 사용하는 job에서만 접근 가능합니다. deploy job에 `environment: production`이 설정되어 있어야 합니다.

## EC2 사전 설정 사항

배포가 정상적으로 작동하려면 EC2 인스턴스에 다음이 설치되어 있어야 합니다:

```bash
# Docker 설치
sudo yum update -y
sudo yum install docker -y
sudo service docker start
sudo usermod -a -G docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# AWS CLI 설치 (ECR 로그인용)
sudo yum install aws-cli -y
```

## 보안 권장사항

1. **최소 권한 원칙**: IAM 사용자에게 필요한 최소한의 권한만 부여
2. **정기적 키 교체**: AWS 액세스 키와 비밀번호는 정기적으로 교체
3. **비밀번호 복잡도**: 모든 비밀번호는 대소문자, 숫자, 특수문자 포함
4. **별도 환경 분리**: 개발/스테이징/프로덕션 환경별로 다른 Secrets 사용

## 문제 해결

### ECR 로그인 실패
- IAM 권한 확인
- AWS_REGION이 ECR 리포지토리 리전과 일치하는지 확인

### SSH 접속 실패
- EC2 보안 그룹에서 GitHub Actions IP 범위 허용 필요
- SSH 키 형식 확인 (개행 문자 포함 전체 복사)

### 헬스체크 실패
- EC2 보안 그룹에서 애플리케이션 포트(9000) 열려있는지 확인
- 도커 컨테이너 로그 확인: `docker-compose logs user-service`