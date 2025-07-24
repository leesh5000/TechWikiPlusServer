# TechWikiPlus 배포 스크립트

이 디렉토리는 TechWikiPlus 프로젝트의 배포 및 운영 관련 스크립트를 포함합니다.

## 📁 스크립트 목록

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

**주요 기능:**
- **스마트 이미지 관리**: 변경된 서비스만 선택적 업데이트
- **Rolling Update**: 무중단 배포 지원
- **자동 헬스체크**: 배포 후 서비스 상태 검증
- **이미지 정리**: 오래된 이미지 자동 삭제
- **배포 통계**: 소요 시간, 업데이트된 서비스 등 상세 정보 제공

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
cd ~/techwikiplus-server
# 새 이미지로 배포
./deploy.sh "123456789.dkr.ecr.ap-northeast-2.amazonaws.com/techwikiplus/server/user-service:20250124-abc1234"

# 환경변수만 업데이트 (기존 이미지 사용)
./deploy.sh
```

## deploy.sh 상세 설명

### 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `PROJECT_DIRECTORY` | `techwikiplus-server` | 프로젝트 디렉토리 이름 |
| `HEALTH_CHECK_URL` | `http://localhost:9000/actuator/health` | 헬스체크 URL |
| `HEALTH_CHECK_MAX_RETRIES` | `10` | 헬스체크 최대 재시도 횟수 |
| `HEALTH_CHECK_RETRY_DELAY` | `5` | 헬스체크 재시도 간격(초) |

### 배포 프로세스

1. **준비 단계**
   - 시스템 상태 확인 (Docker 버전, 디스크/메모리)
   - 현재 실행 중인 이미지 태그 저장 (롤백용)
   - ECR 로그인

2. **변경 사항 확인**
   - 각 서비스별로 이미지 변경 여부 확인
   - 변경된 서비스만 업데이트 대상으로 선택

3. **이미지 Pull**
   - 새 이미지 태그가 제공된 경우 pull
   - 최대 3회 재시도 로직
   - 실패 시 배포 중단

4. **Rolling Update**
   - MySQL, Redis 먼저 업데이트 (의존성 고려)
   - User Service 무중단 배포
   - 컨테이너 초기화 대기 (30초)

5. **헬스체크**
   - 서비스 시작 후 자동 헬스체크
   - 포트 연결 확인
   - HTTP 상태 코드 검증 (200/204)
   - 실패 시 상세 로그 출력

6. **이미지 정리**
   - Dangling 이미지 삭제
   - 각 저장소별로 최신 2개 이미지만 유지
   - 사용 중인 이미지는 보존

### 출력 정보

- 시스템 상태 (Docker 버전, 리소스 사용량)
- 변경된 서비스 목록
- 이미지 pull 진행 상황
- 헬스체크 결과
- 서비스별 실행 상태
- 배포 통계 (소요 시간, 정리된 이미지 수)

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
cat ~/techwikiplus-server/.env
```

### 롤백 방법

배포 실패 시 이전 이미지로 롤백:

```bash
# 현재 .env에서 이전 이미지 태그 백업 확인
cat ~/techwikiplus-server/.env.bak | grep USER_SERVICE_IMAGE

# 이전 이미지로 재배포
./deploy.sh "이전_이미지_태그"
```

## 📚 참고 문서

- [CI/CD 파이프라인 문서](../docs/github-actions-ci-cd.md)
- [Docker 설정 가이드](../docs/docker-setup.md)
- [GitHub Secrets 설정](../docs/github-secrets-setup.md)