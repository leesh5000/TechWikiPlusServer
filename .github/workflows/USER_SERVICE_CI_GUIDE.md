# User Service CI Pipeline Guide

## 개요
User Service의 GitHub Actions CI 파이프라인은 코드 품질을 보장하고 자동화된 테스트 및 빌드를 수행합니다.

## 실행 조건

### 자동 실행
**Pull Request**: PR이 열리거나 업데이트될 때 (opened, synchronize, reopened)

> **Note**: main 브랜치로의 직접 push는 프로젝트 정책상 금지되어 있으므로, CI는 오직 PR 이벤트에서만 실행됩니다.

### 모니터링 대상 경로
- `common/**` - Common 모듈 변경사항
- `service/user/**` - User Service 변경사항
- `build.gradle.kts` - 루트 빌드 설정
- `settings.gradle.kts` - 프로젝트 구조 설정
- `.github/workflows/user-service-ci.yml` - CI 워크플로우 자체

## CI 단계

### 1. Linter (ktlint)
- **목적**: Kotlin 코드 스타일 검사
- **실행 명령**: `./gradlew ktlintCheck`
- **실패 시 조치**: 
  - Job Summary에 위반 사항 표시
  - `./gradlew ktlintFormat`으로 자동 수정 가능

### 2. Test
- **목적**: 단위 테스트 및 통합 테스트 실행
- **대상 모듈**:
  - `:common:snowflake:test`
  - `:service:user:test`
- **실패 시 조치**:
  - 실패한 테스트 케이스 상세 정보 표시
  - 테스트 리포트 아티팩트로 업로드

### 3. Compile Check
- **목적**: Kotlin 코드 컴파일 검증
- **실행 명령**: `./gradlew :common:snowflake:compileKotlin :service:user:compileKotlin`
- **실패 시 조치**:
  - 컴파일 에러 상세 정보 표시
  - 컴파일 로그 아티팩트로 업로드

### 4. Docker Build & Push
- **목적**: Docker 이미지 빌드 및 ECR 푸시
- **실행 조건**: PR에서 모든 CI 검사 통과 후
- **태그 전략**:
  - `latest`: 최신 이미지
  - `SHA`: 커밋 SHA 태그 (예: `a1b2c3d`)
  - `Version`: 타임스탬프 태그 (예: `202508011430`)

## Job Summary
각 CI 실행 시 GitHub Actions Summary에 다음 정보가 표시됩니다:
- 각 단계별 성공/실패 상태
- 실패 시 상세 에러 메시지
- 테스트 통계 (모듈별 테스트 수, 실패 수)
- 빌드 아티팩트 정보

## Pull Request 코멘트
PR에서는 자동으로 CI 결과가 코멘트로 추가됩니다:
- 각 검사 항목의 성공/실패 상태 (ktlint, 테스트, 컴파일, Docker 빌드)
- Docker 이미지 빌드 결과 및 ECR 푸시 상태
- 워크플로우 실행 링크
- 커밋 해시

> **Note**: Docker 이미지는 PR 단계에서 빌드되어 ECR에 푸시됩니다.

## 캐싱 전략
- Gradle 의존성 캐시 활용
- PR에서는 읽기 전용 캐시 사용
- Main 브랜치에서만 캐시 업데이트

## 트러블슈팅

### ktlint 실패
```bash
# 로컬에서 스타일 검사
./gradlew ktlintCheck

# 자동 수정
./gradlew ktlintFormat
```

### 테스트 실패
```bash
# 특정 모듈 테스트 실행
./gradlew :service:user:test

# 상세 로그와 함께 실행
./gradlew :service:user:test --stacktrace --info
```

### 빌드 실패
```bash
# 클린 빌드
./gradlew clean :service:user:build
```

## 로컬 환경 설정
CI와 동일한 환경에서 테스트하려면:
1. Java 21 설치
2. Gradle 8.5 사용
3. 프로젝트 루트에서 명령 실행

## 향후 개선 사항
- [ ] 테스트 커버리지 리포트 추가
- [ ] SonarQube 통합
- [ ] 성능 벤치마크 추가
- [x] CD 파이프라인 구성 (완료)
- [x] CI 중복 실행 방지 (2025-08-01 완료)