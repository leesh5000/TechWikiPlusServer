# 테스트 코드 작성 지침

- 테스트 코드는 반드시 FIRST 원칙을 준수하도록 작성해야 합니다.
    1. **테스트는 독립적**이어야 하며, 다른 테스트에 의존하지 않아야 함
    2. **테스트는 명확하고 이해하기 쉬워야** 하며, 테스트의 목적이 분명히 드러나야 함
    3. **테스트는 충분히 커버리지**를 가져야 하며, 경계 조건 및 예외 상황도 포함해야 함
    4. **테스트는 실행 속도가 빠르도록** 작성해야 하며, 불필요한 지연을 피해야 함

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
