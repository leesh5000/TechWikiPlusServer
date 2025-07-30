# User Service Deployment Guide

## 개요
이 가이드는 User Service의 배포 스크립트(`deploy.sh`) 사용 방법을 설명합니다.

## 배포 스크립트 기능

### 주요 단계
1. **Docker & Docker Compose 설치 확인**
2. **AWS ECR 접속 가능 여부 확인**
3. **필수 설정 파일 존재 확인**
4. **Docker Compose를 통한 서비스 배포**
5. **사용하지 않는 Docker 이미지 정리**
6. **컨테이너 상태 확인**
7. **애플리케이션 헬스체크**

### 시각적 피드백
- ✅ **SUCCESS**: 성공한 단계
- ❌ **ERROR**: 실패한 단계 (스크립트 중단)
- ⚠️ **WARNING**: 경고 (계속 진행 가능)
- ℹ️ **INFO**: 정보성 메시지

## 사전 요구사항

### 1. 필수 소프트웨어
- Docker
- Docker Compose
- AWS CLI (ECR 사용 시)
- curl (헬스체크용)
- jq (선택사항, JSON 출력 포맷팅)

### 2. 필수 파일
배포 디렉토리에 다음 파일들이 필요합니다:
- `docker-compose.base.yml` - 기본 Docker Compose 설정
- `docker-compose.prod.yml` - 프로덕션 환경 설정
- `.env` - 기본 환경 변수
- `.env.prod` - 프로덕션 환경 변수

### 3. 환경 변수
```bash
# AWS ECR 관련 (선택사항)
export AWS_REGION=ap-northeast-2
export ECR_REGISTRY=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/user-service
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key

# 헬스체크 URL (기본값: http://localhost:8080/health)
export HEALTH_CHECK_URL=http://your-domain.com/health
```

## 사용 방법

### 기본 실행
```bash
cd /path/to/user-service
./deploy.sh
```

### 커스텀 헬스체크 URL 지정
```bash
HEALTH_CHECK_URL=http://api.example.com/health ./deploy.sh
```

## 각 단계별 상세 설명

### 1. Docker & Docker Compose 확인
- Docker 데몬이 실행 중인지 확인
- 설치되지 않은 경우 설치 가이드 제공

### 2. AWS ECR 접속 확인
- AWS CLI 설치 여부 확인
- ECR 로그인 시도 (AWS 환경변수 필요)
- 실패해도 로컬 이미지로 계속 진행 가능

### 3. 설정 파일 확인
- 모든 필수 파일이 존재하는지 검증
- 누락된 파일이 있으면 스크립트 중단

### 4. Docker Compose 배포
- 최신 이미지 pull 및 서비스 시작
- 업데이트된 서비스 목록 표시
- 각 서비스의 이미지 정보 출력

### 5. 이미지 정리
- Dangling 이미지 제거
- 24시간 이상 된 사용하지 않는 이미지 제거
- 확보된 디스크 공간 표시

### 6. 컨테이너 상태 확인
- 모든 서비스의 실행 상태 확인
- Docker 헬스체크 결과 표시 (설정된 경우)

### 7. 애플리케이션 헬스체크
- 30초 대기 후 헬스체크 수행
- HTTP 상태 코드 확인
- JSON 응답 포맷팅 (가능한 경우)

## 문제 해결

### Docker 관련 오류
```bash
# Docker 서비스 시작
sudo systemctl start docker

# Docker 권한 문제
sudo usermod -aG docker $USER
newgrp docker
```

### 포트 충돌
```bash
# 사용 중인 포트 확인
sudo netstat -tlnp | grep :8080

# 특정 프로세스 종료
sudo kill -9 <PID>
```

### 로그 확인
```bash
# 모든 서비스 로그
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml logs -f user-service
```

## 유용한 명령어

### 서비스 관리
```bash
# 서비스 중지
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml down

# 서비스 재시작
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml restart

# 특정 서비스만 재시작
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml restart user-service
```

### 상태 확인
```bash
# 컨테이너 상태
docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml ps

# 리소스 사용량
docker stats
```

## 보안 고려사항

1. **환경 변수 관리**
   - 민감한 정보는 `.env.prod`에 저장
   - `.env.prod`는 Git에 커밋하지 않음
   - 프로덕션 서버에서만 관리

2. **권한 설정**
   - 배포 스크립트는 적절한 사용자 권한으로 실행
   - Docker 소켓 접근 권한 확인

3. **네트워크 보안**
   - 필요한 포트만 외부에 노출
   - 방화벽 규칙 적절히 설정

## CI/CD 통합

이 스크립트는 GitHub Actions CD 파이프라인에서 사용될 예정입니다:
1. CI 단계 성공 후 실행
2. SSH를 통해 배포 서버에서 실행
3. 배포 결과를 GitHub Actions에 리포트

## 향후 개선사항

- [ ] 롤백 기능 추가
- [ ] 블루-그린 배포 지원
- [ ] 배포 전 백업 기능
- [ ] Slack/Discord 알림 통합