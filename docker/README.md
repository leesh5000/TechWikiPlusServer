# Docker 구성 가이드

이 디렉토리는 TechWikiPlus 프로젝트의 모든 Docker 관련 파일들을 포함합니다.

## 📁 디렉토리 구조

```
docker/
├── compose/              # Docker Compose 파일들
│   ├── docker-compose.base.yml    # 공통 서비스 정의 (MySQL, Redis)
│   ├── docker-compose.local.yml   # 로컬 개발 환경 설정
│   └── docker-compose.prod.yml    # 프로덕션 환경 설정
├── dockerfiles/          # Dockerfile들
│   └── user-service.dockerfile    # User Service Dockerfile
└── env/                  # 환경변수 예시 파일들
    ├── .env.local.example        # 로컬 환경변수 예시
    └── .env.prod.example         # 프로덕션 환경변수 예시
```

## 📋 사전 요구사항

- Docker Engine 20.10 이상
- Docker Compose 2.0 이상

## 🚀 빠른 시작

### 1. 환경변수 설정

```bash
# 로컬 환경
cp docker/env/.env.local.example .env.local

# 프로덕션 환경
cp docker/env/.env.prod.example .env.prod
# .env.prod 파일을 열어 실제 값으로 수정
```

### 2. 로컬 개발 환경

#### 시작
```bash
# 빌드 및 실행
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  --env-file .env.local \
  up -d --build

# 빌드 없이 실행
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  --env-file .env.local \
  up -d
```

#### 종료
```bash
# 종료
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  down

# 종료 및 볼륨 삭제
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  down -v
```

### 3. 프로덕션 환경

#### 시작
```bash
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.prod.yml \
  --env-file .env.prod \
  up -d
```

#### 종료
```bash
# Graceful shutdown
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.prod.yml \
  stop -t 10

# 컨테이너 제거
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.prod.yml \
  down
```

### 4. 인프라만 실행 (MySQL + Redis)

#### 시작
```bash
# 로컬 환경 설정으로 실행
docker compose -p techwikiplus-server-infra \
  -f docker/compose/docker-compose.base.yml \
  --env-file .env.local \
  up -d

# 또는 기본값으로 실행
docker compose -p techwikiplus-server-infra \
  -f docker/compose/docker-compose.base.yml \
  up -d
```

#### 종료
```bash
docker compose -p techwikiplus-server-infra \
  -f docker/compose/docker-compose.base.yml \
  down
```

## 📋 유용한 명령어

### 로그 확인
```bash
# 전체 로그
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  logs -f

# 특정 서비스 로그
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  logs -f user-service
```

### 서비스 재시작
```bash
# 전체 재시작
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  restart

# 특정 서비스만 재시작
docker compose -p techwikiplus-server-user-service \
  -f docker/compose/docker-compose.base.yml \
  -f docker/compose/docker-compose.local.yml \
  restart user-service
```

### 컨테이너 접속
```bash
# User Service 쉘 접속
docker exec -it techwikiplus-user-service sh

# MySQL 접속
docker exec -it techwikiplus-mysql mysql -u techwikiplus -p

# Redis CLI
docker exec -it techwikiplus-redis redis-cli -a ${REDIS_PASSWORD}
```

### Docker 리소스 정리
```bash
# 중지된 컨테이너 제거
docker container prune -f

# 사용하지 않는 이미지 제거
docker image prune -f

# 사용하지 않는 볼륨 제거 (주의!)
docker volume prune -f

# 사용하지 않는 네트워크 제거
docker network prune -f

# 전체 시스템 정리 (주의!)
docker system prune -a --volumes -f
```

## 📋 환경변수 설명

### 필수 환경변수
- `MYSQL_ROOT_PASSWORD`: MySQL root 비밀번호
- `MYSQL_DATABASE`: 데이터베이스 이름
- `MYSQL_USER`: MySQL 사용자명
- `MYSQL_PASSWORD`: MySQL 비밀번호
- `REDIS_PASSWORD`: Redis 비밀번호
- `JWT_SECRET`: JWT 토큰 서명 키

### 포트 설정
- `MYSQL_PORT`: MySQL 외부 포트 (기본: 13306)
- `REDIS_PORT`: Redis 외부 포트 (기본: 16379)
- `USER_SERVICE_PORT`: User Service 포트 (기본: 9000)

## 📋 Docker Compose 파일 설명

### docker-compose.base.yml
- MySQL과 Redis의 공통 설정
- 네트워크 및 볼륨 정의
- 헬스체크 설정

### docker-compose.local.yml
- 로컬 개발 환경 설정
- 소스 코드 빌드
- 디버그 로깅 활성화
- 모든 Actuator 엔드포인트 노출

### docker-compose.prod.yml
- 프로덕션 환경 설정
- ECR 이미지 사용
- 보안 설정 강화
- 제한된 로깅 및 엔드포인트

## ⚠️ 주의사항

1. **프로덕션 환경**
   - 반드시 강력한 비밀번호 사용
   - JWT_SECRET은 최소 256비트
   - SSL/TLS 인증서 설정 필수

2. **데이터 관리**
   - MySQL 데이터: `mysql-data` 볼륨
   - Redis 데이터: `redis-data` 볼륨
   - 중요 데이터는 정기적으로 백업

3. **포트 충돌**
   - 기본 포트가 사용 중이면 .env 파일에서 변경
   - MySQL: 13306 → 다른 포트
   - Redis: 16379 → 다른 포트
   - User Service: 9000 → 다른 포트

## 🔗 서비스 접속 정보

로컬 환경 기준:
- User Service API: http://localhost:9000
- MySQL: localhost:13306
- Redis: localhost:16379