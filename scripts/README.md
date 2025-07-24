# EC2 배포 스크립트 가이드

이 디렉토리는 EC2 인스턴스 설정 및 배포를 위한 스크립트를 포함합니다.

## 스크립트 설명

### 1. ec2-setup.sh
EC2 인스턴스 초기 설정을 위한 스크립트입니다. **처음 EC2를 설정할 때 한 번만 실행**하면 됩니다.

**수행 작업:**
- Docker 설치
- Docker Compose 설치
- AWS CLI 설치
- .env 파일 템플릿 생성
- 필요한 디렉토리 구조 생성

### 2. deploy.sh
GitHub Actions에서 호출되는 배포 스크립트입니다. CI/CD 파이프라인에서 자동으로 실행됩니다.

**수행 작업:**
- 환경 변수 업데이트
- Docker 이미지 배포
- 서비스 재시작
- 헬스체크

## 사용 방법

### EC2 초기 설정 (최초 1회)

1. EC2 인스턴스에 SSH 접속:
```bash
ssh ec2-user@your-ec2-ip
```

2. 설정 스크립트 다운로드 및 실행:
```bash
# GitHub에서 직접 다운로드
curl -O https://raw.githubusercontent.com/your-repo/TechWikiPlusServer/main/scripts/ec2-setup.sh
chmod +x ec2-setup.sh
./ec2-setup.sh
```

3. .env 파일 수정:
```bash
nano ~/.env
# 실제 환경에 맞게 모든 값을 수정
```

4. Docker 그룹 권한 적용을 위해 재로그인:
```bash
exit
# 다시 SSH 접속
```

### 수동 배포 (필요시)

일반적으로 GitHub Actions가 자동으로 처리하지만, 필요시 수동으로 실행할 수 있습니다:

```bash
cd ~/techwikiplus
./deploy.sh "ECR이미지주소:태그"
```

## 주의사항

1. **보안**: .env 파일에는 민감한 정보가 포함되어 있으므로 권한을 600으로 설정합니다.
2. **IAM 역할**: EC2 인스턴스에 ECR 읽기 권한이 있는 IAM 역할이 필요합니다.
3. **포트**: 보안 그룹에서 9000번 포트가 열려있어야 합니다.

## 문제 해결

### Docker 권한 오류
```bash
# Docker 그룹에 사용자 추가 후 재로그인 필요
sudo usermod -a -G docker $USER
exit
# 다시 접속
```

### ECR 로그인 실패
```bash
# IAM 역할 확인
aws sts get-caller-identity
# ECR 권한이 있는지 확인
```

### 서비스가 시작되지 않을 때
```bash
# 로그 확인
docker-compose logs user-service
# 환경 변수 확인
cat ~/techwikiplus/.env
```