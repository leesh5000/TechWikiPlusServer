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
├── env/                  # 환경변수 예시 파일들
│   ├── .env.local.example        # 로컬 환경변수 예시
│   └── .env.prod.example         # 프로덕션 환경변수 예시
└── scripts/              # 유틸리티 스크립트
    ├── unix/             # Linux/Mac용 Bash 스크립트
    │   ├── common.sh           # 공통 설정 및 함수
    │   ├── docker-manager.sh   # 🎯 통합 관리 도구
    │   ├── build-local.sh      # 로컬 환경 빌드 및 실행
    │   ├── build-prod.sh       # 프로덕션 환경 실행
    │   ├── stop-local.sh       # 로컬 환경 종료
    │   ├── stop-prod.sh        # 프로덕션 환경 종료
    │   ├── restart.sh          # 서비스 재시작
    │   ├── infra-start.sh      # 인프라만 실행
    │   ├── infra-stop.sh       # 인프라만 종료
    │   └── cleanup.sh          # Docker 리소스 정리
    ├── windows/          # Windows용 PowerShell 스크립트
    │   ├── common.ps1          # 공통 설정 및 함수
    │   ├── docker-manager.ps1  # 🎯 통합 관리 도구
    │   ├── build-local.ps1     # 로컬 환경 빌드 및 실행
    │   ├── build-prod.ps1      # 프로덕션 환경 실행
    │   ├── stop-local.ps1      # 로컬 환경 종료
    │   ├── stop-prod.ps1       # 프로덕션 환경 종료
    │   ├── restart.ps1         # 서비스 재시작
    │   ├── infra-start.ps1     # 인프라만 실행
    │   ├── infra-stop.ps1      # 인프라만 종료
    │   └── cleanup.ps1         # Docker 리소스 정리
    └── cross-platform/   # 크로스 플랫폼 진입점
        ├── start.sh             # Unix/Linux/Mac용 진입점
        ├── start.bat            # Windows Batch 진입점
        └── start.ps1            # Windows PowerShell 진입점
```

## 📋 사전 요구사항

### Unix/Linux/Mac
- Docker Engine 20.10 이상
- Docker Compose 2.0 이상
- Bash 쉘

### Windows
- Docker Desktop for Windows
- PowerShell 5.1 이상 또는 PowerShell Core 7+
- Windows 10/11 또는 Windows Server 2016+

#### Windows PowerShell 실행 정책 설정
```powershell
# 관리자 권한으로 실행
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## 🚀 빠른 시작

### 통합 관리 도구 (권장) - 크로스 플랫폼

OS에 맞는 통합 관리 도구를 자동으로 실행:

#### Unix/Linux/Mac
```bash
# 실행 권한 부여 (최초 1회)
chmod +x docker/scripts/unix/*.sh
chmod +x docker/scripts/cross-platform/*.sh

# 실행
./docker/scripts/cross-platform/start.sh
# 또는 직접 실행
./docker/scripts/unix/docker-manager.sh
```

#### Windows
```powershell
# PowerShell에서 실행
.\docker\scripts\cross-platform\start.ps1

# 또는 명령 프롬프트에서 실행
.\docker\scripts\cross-platform\start.bat

# 또는 직접 실행
.\docker\scripts\windows\docker-manager.ps1
```

이 도구는 다음 기능을 제공합니다:
- 🚀 서비스 시작/종료/재시작
- 📊 상태 확인 및 로그 보기
- 🧹 Docker 리소스 정리
- 🔧 고급 옵션 (쉘 접속, DB 접속 등)

### 개별 스크립트 사용

## Unix/Linux/Mac

### 1. 로컬 개발 환경

#### 시작
```bash
# 환경변수 설정
cp docker/env/.env.local.example .env.local

# 스크립트로 실행 (권장)
./docker/scripts/unix/build-local.sh
```

#### 종료
```bash
# 안전하게 종료
./docker/scripts/unix/stop-local.sh
```

#### 재시작
```bash
# 서비스 재시작
./docker/scripts/unix/restart.sh
```

### 2. 프로덕션 환경

#### 시작
```bash
# 환경변수 설정 (필수 값들을 실제 값으로 변경)
cp docker/env/.env.prod.example .env.prod
vim .env.prod

# 스크립트로 실행 (권장)
./docker/scripts/unix/build-prod.sh
```

#### 종료
```bash
# 프로덕션 안전 종료 (Graceful shutdown)
./docker/scripts/unix/stop-prod.sh
```

### 3. 인프라만 실행

MySQL과 Redis만 실행하고 싶을 때:

#### 시작
```bash
# 스크립트 사용 (권장) - 환경 선택 가능
./docker/scripts/unix/infra-start.sh
```

#### 종료
```bash
# 스크립트 사용 (권장) - 선택적 볼륨 관리
./docker/scripts/unix/infra-stop.sh
```

#### 직접 실행
```bash
# 시작
docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml --env-file .env.local up -d

# 종료
docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml down
```

## Windows

### 1. 로컬 개발 환경

#### 시작
```powershell
# 환경변수 설정
copy docker\env\.env.local.example .env.local

# 스크립트로 실행 (권장)
.\docker\scripts\windows\build-local.ps1
```

#### 종료
```powershell
# 안전하게 종료
.\docker\scripts\windows\stop-local.ps1
```

#### 재시작
```powershell
# 서비스 재시작
.\docker\scripts\windows\restart.ps1
```

### 2. 프로덕션 환경

#### 시작
```powershell
# 환경변수 설정 (필수 값들을 실제 값으로 변경)
copy docker\env\.env.prod.example .env.prod
notepad .env.prod

# 스크립트로 실행 (권장)
.\docker\scripts\windows\build-prod.ps1
```

#### 종료
```powershell
# 프로덕션 안전 종료 (Graceful shutdown)
.\docker\scripts\windows\stop-prod.ps1
```

### 3. 인프라만 실행

MySQL과 Redis만 실행하고 싶을 때:

#### 시작
```powershell
# 스크립트 사용 (권장) - 환경 선택 가능
.\docker\scripts\windows\infra-start.ps1
```

#### 종료
```powershell
# 스크립트 사용 (권장) - 선택적 볼륨 관리
.\docker\scripts\windows\infra-stop.ps1
```

#### 직접 실행
```powershell
# 시작
docker-compose -p techwikiplus-server-infra -f docker\compose\docker-compose.base.yml --env-file .env.local up -d

# 종료
docker-compose -p techwikiplus-server-infra -f docker\compose\docker-compose.base.yml down
```

## 📋 각 파일 설명

### Docker Compose 파일들

#### `docker-compose.base.yml`
- **용도**: MySQL과 Redis의 공통 설정
- **포트**: 
  - MySQL: 13306
  - Redis: 16379
- **네트워크**: techwikiplus-network (172.20.0.0/16)

#### `docker-compose.local.yml`
- **용도**: 로컬 개발 환경
- **특징**:
  - 로컬 빌드 사용
  - 콘솔 메일 출력
  - DEBUG 로깅 레벨
  - 모든 Actuator 엔드포인트 노출

#### `docker-compose.prod.yml`
- **용도**: 프로덕션 환경
- **특징**:
  - ECR 이미지 사용
  - SMTP 메일 발송
  - 제한된 로깅
  - 제한된 Actuator 엔드포인트

### Dockerfile

#### `user-service.dockerfile`
- **멀티스테이지 빌드**:
  1. 의존성 캐싱 스테이지
  2. 소스 코드 빌드 스테이지
  3. 최종 실행 이미지
- **최적화**: 
  - BuildKit 활용
  - 레이어 캐싱
  - non-root 사용자 실행

### 환경변수 파일들

#### `.env.local.example`
로컬 개발용 기본값이 설정된 환경변수 예시

#### `.env.prod.example`
프로덕션용 환경변수 예시 (실제 값으로 변경 필요)

### 스크립트

#### Unix/Linux/Mac 스크립트 (`docker/scripts/unix/`)

- **`common.sh`**: 모든 스크립트에서 사용하는 공통 설정 및 함수
- **`docker-manager.sh`**: 통합 관리 도구 (대화형 메뉴)
- **`build-local.sh`**: 로컬 개발 환경 자동 설정 및 실행
- **`build-prod.sh`**: 프로덕션 환경 실행 (환경변수 검증 포함)
- **`stop-local.sh`**: 로컬 환경 안전 종료 (선택적 볼륨 제거)
- **`stop-prod.sh`**: 프로덕션 환경 종료 (Graceful shutdown)
- **`restart.sh`**: 서비스별 선택적 재시작
- **`infra-start.sh`**: MySQL과 Redis만 실행
- **`infra-stop.sh`**: 인프라 서비스만 종료
- **`cleanup.sh`**: Docker 리소스 정리

#### Windows 스크립트 (`docker/scripts/windows/`)

- **`common.ps1`**: PowerShell 공통 설정 및 함수
- **`docker-manager.ps1`**: 통합 관리 도구 (대화형 메뉴)
- **`build-local.ps1`**: 로컬 개발 환경 자동 설정 및 실행
- **`build-prod.ps1`**: 프로덕션 환경 실행 (환경변수 검증 포함)
- **`stop-local.ps1`**: 로컬 환경 안전 종료 (선택적 볼륨 제거)
- **`stop-prod.ps1`**: 프로덕션 환경 종료 (Graceful shutdown)
- **`restart.ps1`**: 서비스별 선택적 재시작
- **`infra-start.ps1`**: MySQL과 Redis만 실행
- **`infra-stop.ps1`**: 인프라 서비스만 종료
- **`cleanup.ps1`**: Docker 리소스 정리

#### 크로스 플랫폼 진입점 (`docker/scripts/cross-platform/`)

- **`start.sh`**: Unix/Linux/Mac에서 적절한 docker-manager 실행
- **`start.bat`**: Windows에서 PowerShell 스크립트 실행
- **`start.ps1`**: Windows PowerShell 진입점

## 🔧 유용한 명령어

### 로그 확인

#### Unix/Linux/Mac
```bash
# 로컬 환경
docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml logs -f

# 특정 서비스만
docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml logs -f user-service
```

#### Windows
```powershell
# 로컬 환경
docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml logs -f

# 특정 서비스만
docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml logs -f user-service
```

### 서비스 관리

#### Unix/Linux/Mac
```bash
# 재시작
docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml restart

# 중지
docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml down

# 중지 및 볼륨 삭제
docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml down -v
```

#### Windows
```powershell
# 재시작
docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml restart

# 중지
docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml down

# 중지 및 볼륨 삭제
docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml down -v
```

### 디버깅

#### 공통 (Unix/Linux/Mac/Windows)
```bash
# 컨테이너 쉘 접속
docker exec -it techwikiplus-user-service sh

# MySQL 접속
docker exec -it techwikiplus-mysql mysql -u techwikiplus -p

# Redis CLI (Unix/Linux/Mac)
docker exec -it techwikiplus-redis redis-cli -a ${REDIS_PASSWORD}

# Redis CLI (Windows PowerShell)
docker exec -it techwikiplus-redis redis-cli -a $env:REDIS_PASSWORD
```

## 💡 팁

### Unix/Linux/Mac Alias 설정
`.bashrc` 또는 `.zshrc`에 추가:

```bash
# 로컬 환경
alias dcl='docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.local.yml --env-file .env.local'

# 프로덕션 환경
alias dcp='docker-compose -p techwikiplus-server-user-service -f docker/compose/docker-compose.base.yml -f docker/compose/docker-compose.prod.yml --env-file .env.prod'

# 베이스 (인프라만)
alias dcb='docker-compose -p techwikiplus-server-infra -f docker/compose/docker-compose.base.yml'
```

사용 예:
```bash
dcl up -d --build  # 로컬 빌드 및 실행
dcl logs -f        # 로컬 로그
dcp ps             # 프로덕션 상태
```

### Windows PowerShell 함수 설정
PowerShell 프로필에 추가 (`$PROFILE` 경로 확인):

```powershell
# 로컬 환경
function dcl {
    docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.local.yml --env-file .env.local @args
}

# 프로덕션 환경
function dcp {
    docker-compose -p techwikiplus-server-user-service -f docker\compose\docker-compose.base.yml -f docker\compose\docker-compose.prod.yml --env-file .env.prod @args
}

# 베이스 (인프라만)
function dcb {
    docker-compose -p techwikiplus-server-infra -f docker\compose\docker-compose.base.yml @args
}
```

사용 예:
```powershell
dcl up -d --build  # 로컬 빌드 및 실행
dcl logs -f        # 로컬 로그
dcp ps             # 프로덕션 상태
```

## ⚠️ 주의사항

1. **프로덕션 환경**:
   - 반드시 강력한 비밀번호 사용
   - JWT_SECRET은 최소 256비트
   - SSL/TLS 인증서 설정 필수

2. **데이터 백업**:
   - MySQL: `docker exec techwikiplus-mysql mysqldump ...`
   - Redis: `docker exec techwikiplus-redis redis-cli BGSAVE`

3. **리소스 관리**:
   - 정기적으로 `docker system prune` 실행
   - 로그 파일 크기 모니터링

4. **Windows 환경**:
   - PowerShell 실행 정책이 RemoteSigned 이상이어야 함
   - Docker Desktop이 실행 중이어야 함
   - 경로 구분자로 백슬래시(`\`) 사용
   - 환경변수 참조 시 `$env:VARIABLE_NAME` 형식 사용

5. **크로스 플랫폼 호환성**:
   - 스크립트 실행 전 실행 권한 확인
   - 환경변수 파일은 LF 줄바꿈 문자 사용 권장
   - Docker Compose 파일의 경로는 슬래시(`/`) 사용

## 🔗 관련 문서

- [전체 Docker 설정 가이드](../docs/docker-setup.md)
- [GitHub Actions CI/CD](../docs/github-actions-ci-cd.md)
- [프로젝트 README](../README.md)
