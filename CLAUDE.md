# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 구조

TechWikiPlus는 멀티모듈 Kotlin Spring Boot 프로젝트입니다:

- **common/**: 공통 모듈
  - `common/snowflake/`: 분산 ID 생성기 (Twitter Snowflake 기반)
- **service/**: 마이크로서비스 모듈
  - `service/user/`: 사용자 관리 서비스 (JWT 인증, Spring Security, JPA, Redis)

각 서비스는 헥사고날 아키텍처를 따라 domain/application/interfaces 계층으로 구성됩니다.

## 주요 명령어

### 빌드 및 테스트
```bash
# 전체 빌드 (테스트 포함)
./gradlew build

# 특정 서비스 빌드
./gradlew :service:user:build

# 테스트만 실행
./gradlew test

# 특정 모듈 테스트 
./gradlew :service:user:test
./gradlew :common:snowflake:test

# 단일 테스트 클래스 실행
./gradlew :service:user:test --tests="*UserRegisterTest*"
```

### 코드 품질
```bash
# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 수정  
./gradlew ktlintFormat

# Git pre-commit hook 설치
./gradlew addKtlintCheckGitPreCommitHook
```

### API 문서 생성
```bash
# OpenAPI 문서 생성 (테스트 실행 후 자동 복사)
./gradlew :service:user:test

# 수동으로 OpenAPI 문서만 생성
./gradlew :service:user:openapi3
```

### Docker 빌드
```bash
# User Service Docker 이미지 빌드
docker build -f service/user/Dockerfile -t techwikiplus-user:latest .
```

## 코딩 규칙

### Kotlin 코딩 스타일
- `Enum.values()` 대신 `Enum.entries` 사용 (Kotlin 1.9+)
- Wildcard import 금지
- ktlint 규칙 준수 필수

### 테스트 코드 규칙
- **Kotest 프레임워크** 사용
- **FIRST 원칙** 준수 (Fast, Independent, Repeatable, Self-validating, Timely)
- **테스트 격리성** 보장
- 단위 테스트는 **Fake 객체** 사용 (예: `FakeUserRepository`, `FakeClockHolder`)
- 통합 테스트는 **TestContainers** 사용 (MySQL, Redis)
- E2E 테스트는 `BaseE2eTest` 상속하여 Spring REST Docs 자동 생성

### 아키텍처 패턴
- **헥사고날 아키텍처** (포트-어댑터 패턴)
- **도메인 주도 설계** (DDD) 적용
- **CQRS** 패턴 (Facade 계층에서 명령/조회 분리)
- **의존성 역전** 원칙 준수 (포트 인터페이스 사용)

### 인증/보안
- JWT 토큰 기반 인증
- Spring Security 설정
- 비밀번호 BCrypt 암호화
- Redis를 통한 토큰 캐싱

## 데이터베이스

- **운영**: MySQL 8.0
- **마이그레이션**: Flyway (`src/main/resources/db/migration/`)
- **테스트**: TestContainers MySQL

마이그레이션 파일 네이밍: `V{버전}__{설명}.sql` (예: `V1.1.0__create_document_schema.sql`)

## API 문서

- **Spring REST Docs** + **OpenAPI 3.0** 통합
- 테스트 실행 시 자동으로 `openapi3.yml` 생성
- 생성된 문서는 Git에 커밋하여 Docker 빌드 시 포함

## 커밋 규칙

커밋 조건 (모두 충족해야 함):
1. 모든 테스트 통과
2. 컴파일러/린터 경고 모두 해결  
3. 하나의 논리적 변경 단위
4. 커밋 메시지에 구조/동작 변경 여부 명시

커밋 메시지는 한글로 작성하고 템플릿을 따릅니다.

## GitHub 템플릿

- 이슈: `.github/ISSUE_TEMPLATE/` 디렉토리 템플릿 사용
- PR: `.github/PULL_REQUEST_TEMPLATE.md` 가이드 준수
