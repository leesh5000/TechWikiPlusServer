# TechWikiPlus Server

TechWikiPlus 프로젝트의 백엔드 서버입니다. 멀티 모듈 입니다.

## 프로젝트 구조

```
TechWikiPlusServer/
├── common/                 # 공통 모듈
│   └── snowflake/         # 분산 ID 생성기
├── service/               # 서비스 모듈
│   └── user/             # 사용자 서비스
├── gradle/               # Gradle Wrapper
├── build.gradle.kts      # 루트 빌드 설정
└── settings.gradle.kts   # 프로젝트 설정
```

### 모듈 설명

- **common**: 모든 서비스에서 공통으로 사용하는 모듈
  - `snowflake`: Twitter Snowflake 기반의 분산 ID 생성기
- **service**: 마이크로서비스
  - `user`: 사용자 관리 서비스

## 새로운 Common 모듈 추가 시 업데이트 필요 항목

새로운 Common 모듈을 추가할 때 아래 파일들을 업데이트해야 합니다:

### 1. `settings.gradle.kts`
프로젝트에 Gradle 모듈을 추가합니다.

```kotlin
include(
    "common",
    "common:snowflake",
    "common:새모듈",  // 추가
    "service",
    "service:user",
)
```

### 2. `service/user/Dockerfile`
Docker 빌드 시 새 모듈의 파일을 포함시켜야 합니다.

#### Stage 1: Dependencies (27-28줄 부근)
```dockerfile
# 프로젝트 빌드 파일 복사
# common 모듈의 빌드 설정
COPY --chown=gradle:gradle common/build.gradle.kts /workspace/common/
COPY --chown=gradle:gradle common/snowflake/build.gradle.kts /workspace/common/snowflake/
COPY --chown=gradle:gradle common/새모듈/build.gradle.kts /workspace/common/새모듈/  # 추가
```

#### Stage 1: 빈 디렉토리 생성 (33줄 부근)
```dockerfile
# Gradle이 빌드할 수 있도록 빈 소스 디렉토리 생성
RUN mkdir -p common/snowflake/src/main/kotlin \
    common/새모듈/src/main/kotlin \  # 추가
    service/user/src/main/kotlin
```

### 3. `service/user/build.gradle.kts` (선택사항)
해당 모듈을 사용하는 서비스에서 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(project(":common:snowflake"))
    implementation(project(":common:새모듈"))  // 추가
    // ... 기타 의존성
}
```

### 4. 새 모듈 디렉토리 구조 생성

```bash
# 새 모듈 디렉토리 생성
mkdir -p common/새모듈/src/main/kotlin/me/helloc/techwikiplus/새모듈
mkdir -p common/새모듈/src/test/kotlin/me/helloc/techwikiplus/새모듈

# build.gradle.kts 생성
cat > common/새모듈/build.gradle.kts << 'EOF'
group = "${rootProject.group}.새모듈"

dependencies {
    // 필요한 의존성 추가
}
EOF
```

## 빌드 방법

### Gradle 빌드

```bash
# 전체 프로젝트 빌드
./gradlew build

# 특정 서비스만 빌드
./gradlew :service:user:build

# 테스트 실행
./gradlew test

# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 수정
./gradlew ktlintFormat
```

### Docker 빌드

```bash
# 프로젝트 루트에서 실행
docker build -f service/user/Dockerfile -t techwikiplus-user:latest .

# BuildKit 사용 (권장)
DOCKER_BUILDKIT=1 docker build -f service/user/Dockerfile -t techwikiplus-user:latest .
```

### Docker Compose 실행

```bash
# 개발 환경
cd service/user
docker-compose --env-file .env.base --env-file .env.local -f docker-compose.base.yml -f docker-compose.local.yml up -d

# 프로덕션 환경
docker-compose --env-file .env.base --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml up -d

# 로그 확인
docker-compose --env-file .env.base --env-file .env.local -f docker-compose.base.yml -f docker-compose.local.yml logs -f

# 중지
docker-compose --env-file .env.base --env-file .env.local -f docker-compose.base.yml -f docker-compose.local.yml down
```

## CI/CD (GitHub Actions)

### CI 파이프라인 개요

User Service의 CI 파이프라인은 코드 품질을 보장하고 자동화된 테스트 및 빌드를 수행합니다.
- **워크플로우 파일**: `.github/workflows/user-service-ci.yml`
- **가이드 문서**: `.github/workflows/USER_SERVICE_CI_GUIDE.md`

### 실행 조건

1. **Main 브랜치 Push**: main 브랜치에 코드가 push될 때
2. **Pull Request**: PR이 열리거나 업데이트될 때 (PR 머지 시에는 실행하지 않음)

**모니터링 대상 경로**:
- `common/**` - Common 모듈 변경사항
- `service/user/**` - User Service 변경사항
- `build.gradle.kts` - 루트 빌드 설정
- `settings.gradle.kts` - 프로젝트 구조 설정
- `.github/workflows/user-service-ci.yml` - CI 워크플로우 자체

### CI 단계

1. **Linter (ktlint)** 🎨
   - Kotlin 코드 스타일 검사
   - 실패 시 위반 사항을 Job Summary에 표시
   - 자동 수정: `./gradlew ktlintFormat`

2. **Tests** 🧪
   - Common과 User Service 모듈의 단위/통합 테스트 실행
   - Linter와 병렬로 실행되어 시간 단축
   - 실패 시 상세 테스트 결과 제공

3. **Build** 🔨
   - 애플리케이션 JAR 파일 빌드
   - 성공 시 아티팩트로 업로드 (7일 보관)
   - JAR 파일 크기 및 이름 표시

### 주요 기능

- **GitHub Actions Summary**: 각 단계별 상세 결과를 시각적으로 표시
- **PR 자동 코멘트**: CI 결과를 표 형식으로 PR에 자동 코멘트
- **에러 리포팅**: 실패 시 구체적인 에러 메시지와 해결 방법 제공
- **캐싱 전략**: 
  - Gradle 의존성 캐시로 빌드 속도 최적화
  - PR에서는 읽기 전용 캐시 사용
  - Main 브랜치에서만 캐시 업데이트

### 로컬에서 CI 검증

CI 파이프라인과 동일한 검사를 로컬에서 실행:

```bash
# 코드 스타일 검사
./gradlew ktlintCheck

# 테스트 실행
./gradlew :common:snowflake:test :service:user:test

# 빌드 (테스트 제외)
./gradlew :service:user:build -x test

# 전체 CI 과정 실행
./gradlew ktlintCheck test build
```

## 기술 스택

### 핵심 기술

- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.3.5
- **빌드 도구**: Gradle 8.5
- **JDK**: 21
- **테스트**: Kotest 5.7.2
- **코드 스타일**: ktlint 1.0.1
- **CI/CD**: GitHub Actions

### 코드 스타일

프로젝트는 ktlint를 사용하여 일관된 코드 스타일을 유지합니다.

```bash
# 코드 스타일 검사
./gradlew ktlintCheck

# 자동 수정
./gradlew ktlintFormat

# Git pre-commit hook 설치
./gradlew addKtlintCheckGitPreCommitHook
```

### 환경 변수 설정

서비스별 `.env.example` 파일을 복사하여 환경 변수를 설정합니다.

```bash
cd service/user
cp .env.example .env.base
# .env.base 파일 편집
```

### 주요 환경 변수

- `MYSQL_HOST`: MySQL 호스트 주소
- `MYSQL_PORT`: MySQL 포트
- `MYSQL_DATABASE`: 데이터베이스 이름
- `MYSQL_USER`: MySQL 사용자
- `MYSQL_PASSWORD`: MySQL 비밀번호
- `REDIS_HOST`: Redis 호스트 주소
- `REDIS_PORT`: Redis 포트
- `REDIS_PASSWORD`: Redis 비밀번호
- `SERVER_PORT`: 애플리케이션 서버 포트

## 협업 규칙

### 브랜치 전략

- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `hotfix/*`: 긴급 수정 브랜치

### 커밋 메시지 규칙

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat`: 새로운 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `style`: 코드 포맷팅
- `refactor`: 코드 리팩토링
- `test`: 테스트 추가/수정
- `chore`: 빌드 프로세스 또는 보조 도구 변경

## 라이선스

이 프로젝트는 [MIT 라이선스](LICENSE)를 따릅니다.
