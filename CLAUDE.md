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
