# User Service CI Pipeline Guide

## 개요
User Service의 GitHub Actions CI 파이프라인은 코드 품질을 보장하고 자동화된 테스트 및 빌드를 수행합니다.

## 실행 조건

### 자동 실행
1. **Pull Request**: PR이 열리거나 업데이트될 때 (opened, synchronize, reopened)
   - 모든 CI 검사가 실행됨 (linter, test, compile, docker build)

2. **Push to main**: main 브랜치로 push될 때 (주로 PR 머지)
   - CD 파이프라인 트리거를 위한 최소 실행
   - 머지 커밋인 경우 expensive jobs는 건너뜀

### 머지 커밋 동작
머지 커밋은 Git parent count로 감지됩니다:
- `git rev-list --parents -n1 HEAD | wc -w` > 2 = 머지 커밋
- 머지 커밋에서는 `ci`와 `summary` job만 실행
- `linter`, `test`, `compile-check`는 건너뜀 (PR에서 이미 실행됨)
- 이 방식은 squash merge나 rebase merge와는 다르게 동작함

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

## 향상된 기능

### Job Output 공유
CI job에서 감지한 정보를 다른 job들이 활용:
```yaml
outputs:
  is_merge_commit: ${{ steps.detect-merge.outputs.is_merge_commit }}
```

### 워크플로우 실행 시간
- 커밋 타임스탬프부터 현재까지의 실제 소요 시간 계산
- Summary에 정확한 duration 표시
- 계산 불가 시 "N/A" 표시

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

### 머지 커밋 감지 문제
```bash
# 로컬에서 머지 커밋 확인
git rev-list --parents -n1 HEAD | wc -w
# 결과가 3 이상이면 머지 커밋

# 워크플로우가 머지를 감지하지 못하는 경우:
# 1. fetch-depth: 2가 설정되어 있는지 확인
# 2. Squash merge 사용 시 일반 커밋으로 처리됨
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
- [x] Git 기반 머지 커밋 감지 구현 (2025-08-01 완료)
- [x] 워크플로우 실행 시간 정확도 개선 (2025-08-01 완료)