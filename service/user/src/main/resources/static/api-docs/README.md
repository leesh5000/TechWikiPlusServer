# OpenAPI 문서 관리

## 🔴 중요 사항
이 디렉토리의 `openapi3.yml` 파일은 **Git에 커밋되어 관리**됩니다.

## 📌 배경 및 이유
### 문제 상황
- Docker 빌드 시 성능 최적화를 위해 테스트를 건너뜀 (`gradle bootJar -x test`)
- 테스트가 실행되지 않으면 REST Docs가 OpenAPI 문서를 생성하지 않음
- 운영 환경에서 Swagger UI가 문서를 찾지 못해 오류 발생

### 해결 방법
- OpenAPI 문서를 Git에 커밋하여 빌드 시 JAR에 포함되도록 함
- 빌드 성능을 유지하면서 운영 환경 문제 해결

## 📝 문서 업데이트 방법

### 1. API 변경 후 테스트 실행
```bash
./gradlew test
# 또는 특정 서비스만
./gradlew :service:user:test
```

### 2. 생성된 문서 확인
- 파일 위치: `src/main/resources/static/api-docs/openapi3.yml`
- 테스트 실행 시 자동으로 업데이트됨

### 3. 변경사항 커밋
```bash
git add src/main/resources/static/api-docs/openapi3.yml
git commit -m "docs: API 문서 업데이트"
```

## ⚠️ 주의사항
1. **API 변경 시 반드시 문서도 함께 업데이트**
   - 테스트를 실행하지 않으면 문서가 구버전으로 유지됨
   
2. **PR 리뷰 시 OpenAPI 문서 변경사항 검토**
   - API 변경과 문서 변경이 일치하는지 확인
   
3. **문서 생성 실패 시**
   - 테스트가 모두 통과하는지 확인
   - REST Docs 스니펫이 정상 생성되는지 확인
   - `build/generated-snippets/` 디렉토리 확인

## 🔧 문서 생성 프로세스
1. E2E 테스트 실행 → REST Docs 스니펫 생성
2. `openapi3` 태스크 → 스니펫을 OpenAPI 형식으로 변환
3. `copyOpenApiToResources` 태스크 → 리소스 디렉토리에 복사
4. 개발자가 Git에 커밋 → 운영 배포 시 포함

## 📚 관련 설정
- Gradle 설정: `build.gradle.kts`의 `openapi3` 섹션
- Swagger UI 설정: `application.yml`의 `springdoc` 섹션
- 정적 리소스 라우팅: `StaticRoutingConfiguration.kt`

## 🔗 접속 경로
- OpenAPI YAML: http://localhost:9000/api-docs/openapi3.yml
- Swagger UI: http://localhost:9000/swagger-ui/index.html

---
*이 문서는 운영 환경 Swagger UI 오류 해결을 위해 2025-08-11에 작성되었습니다.*