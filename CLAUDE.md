# 아키텍처 지침

- **클린 아키텍처** 패턴을 준수하시오.
  - 도메인 로직은 **비즈니스 규칙**에 집중하고, 외부 의존성(예: 데이터베이스, UI 등)은 인터페이스를 통해 분리합니다.
  - 의존성 역전 원칙(DIP)을 준수하여, 상위 모듈이 하위 모듈에 의존하지 않도록 합니다.
  - 도메인 모델은 **불변 객체**로 설계하여 상태 변경을 최소화합니다.
  - 도메인 서비스는 **단일 책임 원칙(SRP)**을 준수하여, 하나의 서비스가 하나의 비즈니스 기능만을 담당하도록 합니다.
- 아래 표는 클린 아키텍처의 각 계층과 그 책임, 외부 의존 가능 여부를 정리한 것입니다.

| 계층                                 | 책임                        | 외부 의존 가능 여부           |
|------------------------------------|---------------------------|-----------------------|
| **Domain Layer**                   | 비즈니스 규칙 (도메인 모델, 도메인 서비스) | ❌ 불가 (완전히 순수)         |
| **Application Layer**              | 유스케이스 실행 흐름, 트랜잭션, 도메인 조작 | ⭕ 일부 가능 (트랜잭션, AOP 등) |
| **Interface/Infrastructure Layer** | 웹, DB, 메시징 등 외부 시스템과의 연결  | ⭕ 필수                  |

# Kotlin 지침

- 'Enum.values()' is recommended to be replaced by 'Enum.entries' since 1.9

# 커밋 지침

- 커밋 조건:
  1. **모든 테스트가 통과**해야 함
  2. **컴파일러/린터 경고가 모두 해결**되어야 함
  3. **하나의 논리 단위 변경**만 포함되어야 함
  4. 커밋 메시지는 구조/동작 변경 여부를 **명확히 기재**
- 커밋은 자주, 작게 나누어 수행해야 하며, 다음 템플릿을 반드시 사용합니다.
- Only commit when:
  1. ALL tests are passing
  2. ALL compiler/linter warnings have been resolved
  3. The change represents a single logical unit of work
  4. Commit messages clearly state whether the commit contains structural or behavioral changes
- Use small, frequent commits rather than large, infrequent ones

# GitHub 이슈 작성 지침

- GitHub 이슈는 반드시 프로젝트의 `.github/ISSUE_TEMPLATE/` 디렉토리에 있는 템플릿을 사용하여 작성하시오.

# PR 지침

- PR 작성 시, 반드시 프로젝트의 `.github/PULL_REQUEST_TEMPLATE.md` 파일의 지침 사항을 준수하시오.

# 분산 추적 (Distributed Tracing) 지침

프로젝트에 Correlation ID 기반의 분산 추적 시스템이 구현되어 있습니다. 

## 주요 컴포넌트

1. **CorrelationId (Domain Layer)**
   - 불변 값 객체로 UUID 형식의 correlation ID를 표현
   - 위치: `domain.correlation.CorrelationId`

2. **CorrelationIdFilter (Infrastructure Layer)**
   - 모든 HTTP 요청에 대해 Correlation ID를 설정하고 MDC에 추가
   - 요청 헤더에 `X-Correlation-ID`가 있으면 사용, 없으면 새로 생성
   - 위치: `infrastructure.correlation.CorrelationIdFilter`

3. **CorrelationIdCoroutineContext (Infrastructure Layer)**
   - 코루틴 컨텍스트 전환 시 MDC의 Correlation ID를 전파
   - 위치: `infrastructure.correlation.CorrelationIdCoroutineContext`

## 사용 방법

### 1. 일반 로깅
```kotlin
private val logger = KotlinLogging.logger {}

logger.info { "Processing request" } // 자동으로 correlation ID가 포함됨
```

### 2. 코루틴에서 로깅
```kotlin
import me.helloc.techwikiplus.infrastructure.correlation.mdcContextWithCorrelationId

suspend fun processAsync() = withContext(Dispatchers.IO + mdcContextWithCorrelationId()) {
    logger.info { "Processing in coroutine" } // correlation ID가 전파됨
}
```

### 3. 로그 출력 형식
- 개발 환경: `2024-01-01 12:00:00.000 [main] INFO  c.e.Service [550e8400-e29b-41d4] - Message`
- 운영 환경: JSON 형식으로 구조화된 로그 출력

## 주의사항

1. 코루틴 사용 시 반드시 `mdcContextWithCorrelationId()` 또는 `correlationIdContext()`를 사용하여 MDC 컨텍스트를 전파
2. 외부 서비스 호출 시 `X-Correlation-ID` 헤더에 현재 correlation ID를 포함하여 전파
3. 로그 분석 도구에서 `correlationId` 필드로 요청 추적 가능
