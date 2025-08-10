# CLAUDE CODE 지침

## Kotlin 지침

- 'Enum.values()' is recommended to be replaced by 'Enum.entries' since 1.9 
- Wildcard import (cannot be auto-corrected) is not allowed.

## 테스트 코드 작성 지침

- 반드시 테스트 코드의 FIRST 원칙을 준수하시오.
- 테스트 코드의 격리성을 보장하시오.

### 단위 테스트 코드

- kotest 라이브러리를 사용하시오.
- 외부 라이브러리는 Fake 객체를 사용하시오.

## 커밋 지침

- 커밋 조건:
  1. **모든 테스트가 통과**해야 함
  2. **컴파일러/린터 경고가 모두 해결**되어야 함
  3. **하나의 논리 단위 변경**만 포함되어야 함
  4. 커밋 메시지는 구조/동작 변경 여부를 **명확히 기재**
- 커밋은 자주, 작게 나누어 수행해야 하며, 다음 템플릿을 반드시 사용합니다.

## GitHub 이슈 작성 지침

- GitHub 이슈는 반드시 프로젝트의 `.github/ISSUE_TEMPLATE/` 디렉토리에 있는 템플릿을 사용하여 작성하시오.

## PR 지침

- PR 작성 시, 반드시 프로젝트의 `.github/PULL_REQUEST_TEMPLATE.md` 파일의 지침 사항을 준수하시오.
