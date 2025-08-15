# syntax=docker/dockerfile:1
# 이 Dockerfile은 멀티 스테이지 빌드를 사용하여 최적화된 이미지를 생성합니다.
#
# 중요: 이 Dockerfile은 프로젝트 루트를 빌드 컨텍스트로 사용합니다!
# 빌드 명령어 예시:
#   프로젝트 루트에서: docker build -f service/api-gateway/Dockerfile .
#   또는 docker-compose 사용 (context: ../.. 설정 필요)
#
# 빌드 인자 (Build arguments)
ARG GRADLE_VERSION=8.14.2
ARG JDK_VERSION=21

# ===== Stage 1: Dependencies caching =====
# 의존성 다운로드를 별도 스테이지로 분리하여 캐싱 효율 극대화
FROM gradle:${GRADLE_VERSION}-jdk${JDK_VERSION}-jammy AS dependencies

WORKDIR /workspace

# Gradle 설정 파일 복사 (프로젝트 루트 기준)
# 캐싱 최적화: 의존성 관련 파일만 먼저 복사
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts /workspace/
COPY --chown=gradle:gradle gradle /workspace/gradle
COPY --chown=gradle:gradle gradlew* /workspace/

# 서브프로젝트 빌드 파일 복사
# common 모듈들의 build.gradle.kts 파일 복사
# 주의: common 디렉토리 자체는 build.gradle.kts가 없고, 하위 모듈만 있음
COPY --chown=gradle:gradle common/snowflake/build.gradle.kts /workspace/common/snowflake/
# 향후 common 하위에 새 모듈 추가 시 여기에 추가
# 예: COPY --chown=gradle:gradle common/새모듈/build.gradle.kts /workspace/common/새모듈/

# api-gateway 서비스의 빌드 설정
COPY --chown=gradle:gradle service/api-gateway/build.gradle.kts /workspace/service/api-gateway/

# Gradle이 빌드 가능하도록 빈 소스 디렉토리 생성
# common 하위 모듈들의 디렉토리 구조 생성
RUN mkdir -p common/snowflake/src/main/kotlin service/api-gateway/src/main/kotlin

# 의존성 다운로드 (캐시 마운트 활용)
# --mount=type=cache: Docker BuildKit의 캐시 마운트 기능
# 재빌드 시 의존성을 다시 다운로드하지 않음
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    gradle :service:api-gateway:dependencies --no-daemon --parallel

# ===== Stage 2: Build application =====
# 애플리케이션 빌드 스테이지
FROM gradle:${GRADLE_VERSION}-jdk${JDK_VERSION}-jammy AS build

WORKDIR /workspace

# 이전 스테이지에서 캐싱된 의존성 복사
COPY --from=dependencies --chown=gradle:gradle /workspace /workspace
COPY --from=dependencies --chown=gradle:gradle /home/gradle/.gradle /home/gradle/.gradle

# 소스 코드 복사 (프로젝트 루트 기준)
# common 모듈 (snowflake 등 공통 코드)
COPY --chown=gradle:gradle common /workspace/common
# api-gateway 서비스 소스
COPY --chown=gradle:gradle service/api-gateway /workspace/service/api-gateway

# 애플리케이션 빌드 (테스트 제외)
# bootJar: Spring Boot 실행 가능한 JAR 생성
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    gradle :service:api-gateway:bootJar --no-daemon --parallel -x test

# ===== Stage 3: Runtime =====
# 최종 실행 이미지 (최소 크기)
FROM eclipse-temurin:${JDK_VERSION}-jre-alpine AS runtime

# 필수 패키지 설치 및 non-root 사용자 생성
# Alpine Linux 사용으로 이미지 크기 최소화
# 패키지: curl (헬스체크용), tzdata (시간대 설정용)
RUN apk add --no-cache \
    curl \
    tzdata \
    && addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

# 시간대 설정 (한국 표준시)
ENV TZ=Asia/Seoul

# 애플리케이션 디렉토리 생성
WORKDIR /app

# 빌드된 JAR 파일 복사
# build 스테이지에서 생성된 JAR를 복사
COPY --from=build --chown=appuser:appgroup /workspace/service/api-gateway/build/libs/*.jar app.jar

# 로그 및 임시 파일용 디렉토리 생성
RUN mkdir -p /app/logs /app/temp \
    && chown -R appuser:appgroup /app

# 보안: non-root 사용자로 전환
USER appuser

# 애플리케이션 포트 노출 (API Gateway는 9000번 포트 사용)
EXPOSE 9000 5000

# 헬스 체크 설정
# Spring Boot Actuator의 health 엔드포인트 활용
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9000/actuator/health || exit 1

# JVM 최적화 설정 (컨테이너 환경)
# -XX:+UseContainerSupport: 컨테이너 리소스 제한 인식
# -XX:MaxRAMPercentage: 컨테이너 메모리의 75% 사용
# -XX:+UseG1GC: G1 가비지 컬렉터 사용 (대용량 힙에 적합)
# -XX:+UseStringDeduplication: 문자열 중복 제거로 메모리 절약
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Dfile.encoding=UTF-8"

# 애플리케이션 실행
# JAVA_OPTS 환경 변수를 적용하여 JAR 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS ${DEBUG_OPTS:-} -jar app.jar"]