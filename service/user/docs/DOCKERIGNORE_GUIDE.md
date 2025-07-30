# .dockerignore 가이드

## 현재 상황

`.dockerignore` 파일이 프로젝트 루트로 이동되었습니다.

### 파일 위치
- **이전**: `/service/user/.dockerignore` (사용되지 않음)
- **현재**: `/.dockerignore` (프로젝트 루트 - 올바른 위치)

### 이유
Dockerfile의 빌드 컨텍스트가 프로젝트 루트이기 때문에, `.dockerignore`도 프로젝트 루트에 있어야 합니다.

```dockerfile
# Dockerfile 내용
# 중요: 이 Dockerfile은 프로젝트 루트를 빌드 컨텍스트로 사용합니다!
# 빌드 명령어 예시:
#   프로젝트 루트에서: docker build -f service/user/docker/Dockerfile .
```

## 주요 변경사항

### 1. 경로 패턴 업데이트
모든 경로를 `**/` 패턴으로 변경하여 하위 디렉토리까지 적용:
- `build/` → `**/build/`
- `src/test/` → `**/src/test/`
- `.gradle/` → `**/.gradle/`

### 2. 빌드에 필요한 파일 유지
다음 파일들은 제외하지 않음:
- `gradle/wrapper/gradle-wrapper.jar` - Gradle 빌드에 필요
- `*.jar` 패턴 제거 - Gradle wrapper JAR 보존

## Docker 빌드 최적화 효과

`.dockerignore`를 올바르게 설정함으로써:
1. **빌드 컨텍스트 크기 감소**: 불필요한 파일들이 Docker 데몬으로 전송되지 않음
2. **빌드 속도 향상**: 전송할 파일이 적어져 빌드가 빨라짐
3. **보안 강화**: 환경 파일, 키 파일 등이 이미지에 포함되지 않음

## 확인 방법

```bash
# 프로젝트 루트에서
# 빌드 컨텍스트 크기 확인
du -sh .

# Docker 빌드 테스트
docker build -f service/user/docker/Dockerfile -t test-build .

# 빌드 컨텍스트에 포함되는 파일 확인 (dry-run)
docker build --no-cache --progress=plain -f service/user/docker/Dockerfile . 2>&1 | grep "Sending build context"
```

## 주의사항

1. **service/user/.dockerignore는 삭제 가능**: 더 이상 사용되지 않음
2. **프로젝트 루트의 .dockerignore 유지**: 모든 Docker 빌드에 적용됨
3. **패턴 테스트**: 새로운 제외 패턴 추가 시 빌드에 필요한 파일이 제외되지 않는지 확인