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
