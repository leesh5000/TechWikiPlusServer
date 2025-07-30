# Dockerfile 유지보수 가이드

## 개요
이 문서는 TechWikiPlus User Service의 Dockerfile을 유지보수하는 방법을 설명합니다.

## 프로젝트 구조와 Dockerfile의 관계

### 현재 프로젝트 구조
```
TechWikiPlusServer/
├── build.gradle.kts              # 루트 빌드 설정
├── settings.gradle.kts           # 프로젝트 설정
├── common/                       # 공통 모듈 디렉토리 (build.gradle.kts 없음)
│   └── snowflake/               # Snowflake 모듈
│       └── build.gradle.kts     # Snowflake 빌드 설정
└── service/                      # 서비스 디렉토리
    └── user/                    # User 서비스
        ├── build.gradle.kts     # User 서비스 빌드 설정
        ├── docker/              # Docker 관련 파일
        │   └── Dockerfile       # Docker 빌드 파일
        └── docs/                # 문서 파일
```

### 중요 사항
- `common` 디렉토리는 **build.gradle.kts 파일이 없습니다**
- 각 하위 모듈(예: snowflake)만 build.gradle.kts를 가집니다
- 이는 의도된 설계입니다 (멀티모듈 프로젝트 구조)

## 새로운 Common 모듈 추가 시

### 1. Dockerfile 수정 위치

Dockerfile 위치: `service/user/docker/Dockerfile`

#### 빌드 파일 복사 (28-30번 줄 근처)
```dockerfile
# 기존 모듈
COPY --chown=gradle:gradle common/snowflake/build.gradle.kts /workspace/common/snowflake/

# 새 모듈 추가 예시
COPY --chown=gradle:gradle common/새모듈/build.gradle.kts /workspace/common/새모듈/
```

#### 디렉토리 생성 (37번 줄 근처)
```dockerfile
# 기존
RUN mkdir -p common/snowflake/src/main/kotlin service/user/src/main/kotlin

# 수정 후
RUN mkdir -p common/snowflake/src/main/kotlin \
    common/새모듈/src/main/kotlin \
    service/user/src/main/kotlin
```

### 2. 체크리스트
- [ ] 새 모듈의 build.gradle.kts 파일이 존재하는지 확인
- [ ] Dockerfile에 COPY 명령어 추가
- [ ] mkdir 명령어에 새 모듈 디렉토리 추가
- [ ] 빌드 테스트 수행

## 문제 해결

### "file not found" 에러 발생 시
1. 파일 경로가 정확한지 확인
   ```bash
   # 프로젝트 루트에서 실행
   find . -name "build.gradle.kts" | grep -v build/
   ```

2. 빌드 컨텍스트가 올바른지 확인
   - docker-compose.yml의 `context: ../../..` 설정 확인 (docker 디렉토리 기준)
   - 빌드 명령어가 올바른 위치에서 실행되는지 확인

### 빌드 캐시 문제
```bash
# 캐시 없이 완전 재빌드 (service/user 디렉토리에서 실행)
docker-compose -f docker/docker-compose.prod.yml build --no-cache user

# BuildKit 캐시 정리
docker builder prune
```

## 최적화 팁

### 1. 레이어 캐싱 최대화
- 자주 변경되지 않는 파일을 먼저 복사
- 소스 코드는 가장 마지막에 복사

### 2. 빌드 시간 단축
```bash
# BuildKit 활성화 (빠른 빌드)
DOCKER_BUILDKIT=1 docker-compose build
```

### 3. 이미지 크기 최소화
- 멀티 스테이지 빌드 활용
- 불필요한 파일은 .dockerignore에 추가

## 자주 묻는 질문

### Q: 왜 common/build.gradle.kts가 없나요?
A: common은 단순히 하위 모듈들을 그룹화하는 디렉토리입니다. Gradle 멀티모듈 프로젝트에서는 모든 디렉토리가 빌드 설정을 가질 필요가 없습니다.

### Q: 모든 common 모듈을 자동으로 복사할 수 없나요?
A: Docker의 COPY 명령어는 제한적인 패턴 매칭만 지원합니다. 명시적으로 각 모듈을 지정하는 것이 더 안전하고 예측 가능합니다.

### Q: 빌드가 느린데 어떻게 해야 하나요?
A: BuildKit을 활성화하고, 캐시 마운트를 활용하세요. 현재 Dockerfile은 이미 최적화되어 있습니다.

## 참고 자료
- [Docker 공식 문서 - Dockerfile 베스트 프랙티스](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Gradle 멀티모듈 프로젝트 가이드](https://docs.gradle.org/current/userguide/multi_project_builds.html)