# TechWikiPlus User Service

## Docker 실행 가이드

### 사전 준비

1. Docker 및 Docker Compose 설치 확인
   ```bash
   docker --version
   docker compose version
   ```

2. 환경 변수 설정
   - `config/.env.example` 파일을 참고하여 `.env.base` 파일 생성
   - 필요에 따라 환경 변수 값 수정

### 실행 방법

### Local 개발 환경 (SpringBoot 서버는 IDE에서 실행하는 것을 가정합니다.)

#### 1. 서비스 시작

```bash
# 1. config 디렉토리의 .env.example 파일을 .env.base로 복사 후 설정 값에 맞게 수정
cp config/.env.example .env.base & cp config/.env.local.example .env.local

# 2. Docker Compose로 서비스 시작
docker compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml up -d
```

#### 2. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml ps
```

#### 3. 서비스 중지

```bash
# 서비스 중지 및 컨테이너 제거
docker compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml down

# 볼륨까지 모두 제거 (데이터 초기화)
docker compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml down -v
```

### Production 환경

#### 1. 서비스 시작

```bash
# 1. config 디렉토리의 .env.example 파일을 .env.base로 복사 후 설정 값에 맞게 수정
cp config/.env.example .env.base & cp config/.env.prod.example .env.prod

# 2. Docker Compose로 서비스 시작
docker compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml up -d
```

#### 2. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml ps
```

#### 3. 서비스 중지

```bash
# 서비스 중지 및 컨테이너 제거
docker compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml down

# 볼륨까지 모두 제거 (데이터 초기화)
docker compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml down -v
```
