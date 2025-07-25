# Docker 환경 구성 및 사용 가이드

이 문서는 TechWikiPlus 프로젝트를 Docker 환경에서 실행하는 방법을 설명합니다.

## 목차

- [사전 요구사항](#사전-요구사항)
- [빠른 시작 가이드](#빠른-시작-가이드)
- [상세 설정 설명](#상세-설정-설명)
- [Docker Compose 명령어](#docker-compose-명령어)
- [운영 환경 고려사항](#운영-환경-고려사항)
- [문제 해결 가이드](#문제-해결-가이드)

## 사전 요구사항

- Docker Engine 20.10 이상
- Docker Compose 2.0 이상
- 최소 4GB RAM (권장 8GB)
- 10GB 이상의 여유 디스크 공간

### Docker 설치 확인

```bash
docker --version
docker-compose --version
```

## 빠른 시작 가이드

### 1. 환경 변수 설정

```bash
# 프로젝트 루트 디렉토리에서
cp docs/.env.example .env
```

`.env` 파일을 열어 필수 환경 변수를 설정합니다:

```env
# JWT 비밀키 생성 (필수)
JWT_SECRET=$(openssl rand -base64 32)

# 이메일 설정 (필수)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

### 2. 애플리케이션 빌드 및 실행

#### 방법 1: docker-build.sh 스크립트 사용 (권장)

```bash
# 빌드 스크립트 실행
./docker-build.sh
```

#### 방법 2: Docker Compose 직접 실행

```bash
# BuildKit 활성화 (TestContainers 실행을 위해 필요)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# 모든 서비스 빌드 및 실행
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f user-service
```

> **참고**: BuildKit은 Docker 18.09 이상에서 지원됩니다. TestContainers가 정상 작동하려면 BuildKit과 Docker 소켓 접근이 필요합니다.

### 3. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker-compose ps

# 헬스체크 상태 확인
docker inspect techwikiplus-user-service --format='{{.State.Health.Status}}'
```

### 4. 서비스 접속

- User Service API: http://localhost:9000
- MySQL: localhost:13306
- Redis: localhost:16379

## 상세 설정 설명

### 환경 변수

#### 필수 환경 변수

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `JWT_SECRET` | JWT 토큰 서명용 비밀키 (최소 32자) | `openssl rand -base64 32` 출력값 |
| `MAIL_USERNAME` | SMTP 이메일 주소 | `noreply@example.com` |
| `MAIL_PASSWORD` | SMTP 이메일 비밀번호 | Gmail 앱 비밀번호 |
| `MYSQL_PASSWORD` | MySQL 사용자 비밀번호 | 안전한 비밀번호 |
| `REDIS_PASSWORD` | Redis 비밀번호 | 안전한 비밀번호 |

#### 선택적 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `USER_SERVICE_IMAGE` | `techwikiplus/user-service:latest` | User Service Docker 이미지 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | JPA DDL 자동 생성 모드 |
| `SPRING_MAIL_TYPE` | `console` | 메일 전송 방식 (`smtp` 또는 `console`) |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` | SMTP 서버 호스트 |
| `SPRING_MAIL_PORT` | `587` | SMTP 서버 포트 |
| `MYSQL_ROOT_PASSWORD` | - | MySQL root 비밀번호 |
| `USER_SERVICE_PORT` | `9000` | User Service 포트 |
| `MYSQL_PORT` | `13306` | MySQL 외부 포트 |
| `REDIS_PORT` | `16379` | Redis 외부 포트 |
| `LOGGING_LEVEL_ROOT` | `INFO` | 루트 로깅 레벨 |
| `LOGGING_LEVEL_TECHWIKIPLUS` | `INFO` | 애플리케이션 로깅 레벨 |
| `LOGGING_LEVEL_SPRING_WEB` | `INFO` | Spring Web 로깅 레벨 |
| `LOGGING_LEVEL_SPRING_SECURITY` | `INFO` | Spring Security 로깅 레벨 |
| `LOGGING_LEVEL_HIBERNATE_SQL` | `WARN` | Hibernate SQL 로깅 레벨 |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | `false` | 메일 서버 헬스 체크 활성화 |
| `CORS_ALLOWED_ORIGINS` | `*` | CORS 허용 Origin |

### 볼륨 구성

Docker Compose는 다음 볼륨을 생성합니다:

- `mysql-data`: MySQL 데이터 영구 저장
- `redis-data`: Redis 데이터 영구 저장

### 네트워크 구성

모든 서비스는 `techwikiplus-network` (172.20.0.0/16) 브리지 네트워크에서 실행됩니다.

## Docker Compose 명령어

### 기본 명령어

```bash
# 서비스 시작
docker-compose up -d

# 서비스 중지
docker-compose down

# 서비스 중지 및 볼륨 삭제
docker-compose down -v

# 서비스 재시작
docker-compose restart

# 특정 서비스만 재시작
docker-compose restart user-service
```

### 빌드 관련 명령어

```bash
# 이미지 재빌드
docker-compose build

# 캐시 없이 재빌드
docker-compose build --no-cache

# 특정 서비스만 재빌드
docker-compose build user-service
```

### 로그 및 모니터링

```bash
# 모든 서비스 로그
docker-compose logs

# 실시간 로그 스트리밍
docker-compose logs -f

# 최근 100줄만 표시
docker-compose logs --tail=100

# 특정 서비스 로그
docker-compose logs user-service
```

### 스케일링

```bash
# User Service를 3개 인스턴스로 실행
docker-compose up -d --scale user-service=3
```

## 운영 환경 고려사항

### 1. 보안 설정

운영 환경에서는 다음 사항을 반드시 변경하세요:

- 모든 기본 비밀번호 변경
- 강력한 JWT 비밀키 사용 (최소 512비트)
- SSL/TLS 인증서 적용
- 방화벽 규칙 설정

### 2. 리소스 최적화

```yaml
# docker-compose.override.yml 예시
version: '3.8'

services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 3. 백업 전략

```bash
# MySQL 백업
docker exec techwikiplus-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} techwikiplus > backup.sql

# Redis 백업
docker exec techwikiplus-redis redis-cli --pass ${REDIS_PASSWORD} BGSAVE
```

### 4. 모니터링

Prometheus와 Grafana를 추가하여 모니터링:

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
```

## 문제 해결 가이드

### 1. 개발 워크플로우

이 프로젝트는 로컬 개발과 CI/CD를 분리하여 효율적인 개발 환경을 제공합니다.

#### 로컬 개발 환경
- **빠른 빌드**: 테스트를 제외하여 빌드 시간 단축
- **즉시 실행**: Docker Compose로 전체 환경 구성
- **테스트**: 로컬에서 필요시 `./gradlew test` 실행

#### CI/CD 환경
- **완전한 빌드**: 테스트를 포함한 전체 빌드 프로세스
- **자동화**: GitHub Actions에서 자동으로 테스트 실행
- **품질 보증**: 모든 PR과 커밋에 대해 테스트 수행

### 2. 테스트 실행 방법

#### 로컬에서 테스트
```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :service:user:test

# 특정 테스트 클래스 실행
./gradlew test --tests "*.UserTest"
```

#### CI/CD에서 자동 테스트
GitHub에 푸시하면 자동으로 테스트가 실행됩니다:
- Pull Request 생성 시
- main/develop 브랜치에 푸시 시

### 2. 컨테이너가 시작되지 않는 경우

```bash
# 상세 로그 확인
docker-compose logs user-service

# 컨테이너 상태 확인
docker-compose ps

# 환경 변수 확인
docker-compose config
```

### 2. 데이터베이스 연결 실패

```bash
# MySQL 컨테이너 접속
docker exec -it techwikiplus-mysql mysql -u root -p

# 연결 테스트
docker exec techwikiplus-user-service nc -zv mysql 3306
```

### 3. 메모리 부족

```bash
# Docker 리소스 사용량 확인
docker stats

# 불필요한 이미지 정리
docker system prune -a
```

### 4. 포트 충돌

```bash
# 사용 중인 포트 확인
netstat -tlnp | grep -E '(9000|13306|16379)'

# .env 파일에서 포트 변경
USER_SERVICE_PORT=9001
MYSQL_PORT=13307
REDIS_PORT=16380
```

### 5. 빌드 캐시 문제

```bash
# 모든 캐시 삭제 후 재빌드
docker-compose down
docker system prune -a
docker-compose build --no-cache
docker-compose up -d
```

## 개발 팁

### 1. 로컬 개발 환경과 Docker 환경 전환

```bash
# Docker 환경 사용
export SPRING_PROFILES_ACTIVE=docker

# 로컬 환경 사용
export SPRING_PROFILES_ACTIVE=local
```

### 2. 메일 전송 설정

개발/테스트 환경에서는 실제 이메일을 보내지 않고 콘솔에 로그로 출력할 수 있습니다:

```bash
# 개발 환경: 콘솔에 로그 출력
export SPRING_MAIL_TYPE=console

# 프로덕션 환경: 실제 이메일 발송
export SPRING_MAIL_TYPE=smtp
```

### 3. 로깅 레벨 설정

애플리케이션의 로깅 레벨을 환경에 따라 조정할 수 있습니다:

```bash
# 개발 환경: 상세한 로그
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_TECHWIKIPLUS=DEBUG
export LOGGING_LEVEL_HIBERNATE_SQL=DEBUG

# 프로덕션 환경: 최소한의 로그
export LOGGING_LEVEL_ROOT=WARN
export LOGGING_LEVEL_TECHWIKIPLUS=INFO
export LOGGING_LEVEL_HIBERNATE_SQL=WARN
```

### 4. 컨테이너 내부 접속

```bash
# User Service 컨테이너 쉘 접속
docker exec -it techwikiplus-user-service sh

# MySQL 클라이언트 접속
docker exec -it techwikiplus-mysql mysql -u techwikiplus -p
```

### 5. 실시간 로그 모니터링

```bash
# 여러 서비스 로그 동시 모니터링
docker-compose logs -f user-service mysql redis
```

## 추가 리소스

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)