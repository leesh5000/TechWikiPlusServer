# TechWikiPlus User Service Docker 빌드 가이드

## 개요

이 문서는 TechWikiPlus User Service의 Docker 이미지 빌드 최적화 방법과 사용법을 설명합니다.

## 빌드 최적화 기능

### 1. 멀티 스테이지 빌드
- **3단계 구성**: dependencies → build → runtime
- **이미지 크기 감소**: 최종 이미지에는 실행에 필요한 파일만 포함
- **빌드 시간 단축**: 각 스테이지가 병렬로 캐싱됨

### 2. 빌드 캐시 최적화
```dockerfile
RUN --mount=type=cache,target=/home/gradle/.gradle/caches
```
- Gradle 의존성 캐시를 Docker 빌드 캐시로 마운트
- 재빌드 시 의존성 다운로드 시간 대폭 단축
- CI/CD 환경에서도 캐시 재사용 가능

### 3. 레이어 캐싱 전략
- 변경이 적은 파일을 먼저 복사 (build.gradle.kts, settings.gradle.kts)
- 소스 코드는 마지막에 복사하여 캐시 효율 극대화
- 의존성 변경 없이 코드만 수정 시 빠른 재빌드

### 4. 보안 강화
- Non-root 사용자 (uid: 1001) 실행
- 최소 권한 원칙 적용
- Alpine Linux 기반으로 공격 표면 최소화

### 5. JVM 최적화
```bash
-XX:+UseContainerSupport          # 컨테이너 리소스 인식
-XX:MaxRAMPercentage=75.0         # 메모리 사용률 설정
-XX:+UseG1GC                      # G1 가비지 컬렉터
-XX:+UseStringDeduplication       # 문자열 중복 제거
```

## 빌드 방법

### 기본 빌드
```bash
# 프로젝트 루트에서 실행
cd /path/to/TechWikiPlusServer
docker build -f service/user/docker/Dockerfile -t techwikiplus-user:latest .
```

### 빌드 인자 사용
```bash
# 특정 JDK 버전 사용
docker build \
  --build-arg JDK_VERSION=21 \
  --build-arg GRADLE_VERSION=8.14.2 \
  -f service/user/docker/Dockerfile \
  -t techwikiplus-user:latest .
```

### BuildKit 활용 (권장)
```bash
# BuildKit으로 더 빠른 빌드
DOCKER_BUILDKIT=1 docker build \
  -f service/user/docker/Dockerfile \
  -t techwikiplus-user:latest .
```

## 실행 방법

### 단독 실행
```bash
docker run -d \
  --name techwikiplus-user \
  -p 9000:9000 \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=13306 \
  -e MYSQL_DATABASE=techwikiplus_user \
  -e MYSQL_USER=techwikiplus \
  -e MYSQL_PASSWORD=techwikiplus \
  techwikiplus-user:latest
```

### Docker Compose 실행 및 중지
프로젝트는 base 설정과 환경별 설정을 분리하여 관리합니다.

```bash
# 프로덕션 환경
cd service/user
docker-compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml up -d --build

# 중지
docker-compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml down

# 중지 (볼륨 삭제)
docker-compose --env-file .env.base --env-file .env.prod -f docker/docker-compose.base.yml -f docker/docker-compose.prod.yml down --volumes

# 로컬 환경 (개발)
docker-compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml up -d

# 중지
docker-compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml down

# 중지 (볼륨 삭제)
docker-compose --env-file .env.base --env-file .env.local -f docker/docker-compose.base.yml -f docker/docker-compose.local.yml down --volumes
```

## 성능 최적화 결과

### 이미지 크기
- 기본 OpenJDK 이미지: ~800MB
- 최적화된 Alpine 이미지: ~250MB
- **약 70% 크기 감소**

### 빌드 시간
- 최초 빌드: 2-3분
- 캐시 활용 재빌드: 30초 이내
- 코드만 변경 시: 10초 이내

### 메모리 사용
- 컨테이너 메모리 인식으로 OOM 방지
- G1GC로 안정적인 가비지 컬렉션
- 문자열 중복 제거로 메모리 효율 향상

## 문제 해결

### 빌드 캐시 초기화
```bash
# 캐시 없이 완전 재빌드
docker build --no-cache -f service/user/Dockerfile -t techwikiplus-user:latest .
```

### 빌드 로그 상세 확인
```bash
# BuildKit 진행 상황 표시
DOCKER_BUILDKIT=1 docker build \
  --progress=plain \
  -f service/user/docker/Dockerfile \
  -t techwikiplus-user:latest .
```

### 헬스 체크 실패
```bash
# 컨테이너 로그 확인
docker logs techwikiplus-user

# 헬스 체크 상태 확인
docker inspect techwikiplus-user --format='{{json .State.Health}}'
```

## CI/CD 통합

### GitHub Actions 예시
```yaml
- name: Build and push Docker image
  uses: docker/build-push-action@v4
  with:
    context: .
    file: ./service/user/Dockerfile
    push: true
    tags: |
      ghcr.io/${{ github.repository }}/user-service:latest
      ghcr.io/${{ github.repository }}/user-service:${{ github.sha }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### Jenkins Pipeline 예시
```groovy
stage('Build Docker Image') {
    steps {
        script {
            docker.build("techwikiplus-user:${env.BUILD_NUMBER}", 
                        "-f service/user/Dockerfile .")
        }
    }
}
```

## 보안 권장사항

1. **이미지 스캔**
   ```bash
   # Trivy로 취약점 스캔
   trivy image techwikiplus-user:latest
   ```

2. **시크릿 관리**
   - 환경 변수로 민감한 정보 전달
   - Docker Secrets 또는 Kubernetes Secrets 활용
   - 이미지에 시크릿 하드코딩 금지

3. **네트워크 격리**
   - 전용 Docker 네트워크 사용
   - 필요한 포트만 노출
   - 서비스 간 통신은 내부 네트워크 활용

## 모니터링

### 리소스 사용량 확인
```bash
# CPU, 메모리 사용량 실시간 확인
docker stats techwikiplus-user

# 상세 리소스 정보
docker inspect techwikiplus-user | jq '.[0].HostConfig.Memory'
```

### 로그 수집
```bash
# 로그 확인
docker logs -f techwikiplus-user

# 로그 파일로 저장
docker logs techwikiplus-user > user-service.log 2>&1
```

## 추가 최적화 팁

1. **JAR 최적화**
   - Spring Boot의 layered JAR 활용
   - 불필요한 의존성 제거

2. **베이스 이미지 선택**
   - distroless 이미지 고려
   - JLink로 커스텀 JRE 생성

3. **빌드 파이프라인**
   - 병렬 빌드 활용
   - 분산 캐시 시스템 구축

## 참고 자료

- [Docker 공식 문서 - Multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)
- [JVM 컨테이너 최적화](https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness)
