# 빌드 시간 최적화된 Dockerfile
# 멀티 스테이지 빌드로 캐싱 효율 극대화

# Stage 1: 의존성 캐싱 (거의 변경되지 않음)
FROM gradle:8.5-jdk21-alpine AS dependencies

WORKDIR /app

# Gradle 설정 파일만 먼저 복사 (캐시 최적화)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

# 모듈별 build 파일 복사
COPY common/snowflake/build.gradle.kts ./common/snowflake/
COPY service/user/build.gradle.kts ./service/user/

# 빈 소스 디렉토리 생성 (의존성 다운로드를 위해)
RUN mkdir -p common/snowflake/src/main/kotlin \
    && mkdir -p service/user/src/main/kotlin

# 의존성만 다운로드 (캐시됨)
# 병렬 처리와 빌드 캐시 활성화로 속도 향상
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :service:user:dependencies \
    --no-daemon \
    --parallel \
    --build-cache

# Stage 2: 소스 코드 빌드
FROM dependencies AS builder

# 소스 코드 복사 (자주 변경됨)
COPY common/snowflake/src ./common/snowflake/src
COPY service/user/src ./service/user/src

# 애플리케이션 빌드
# Gradle 캐시 마운트로 재빌드 시간 단축
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :service:user:build -x test \
    --no-daemon \
    --parallel \
    --build-cache \
    -Dorg.gradle.caching=true

# Stage 3: 최종 실행 이미지
FROM eclipse-temurin:21-jre-alpine AS runtime

# 시간대 설정 (캐시됨)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

WORKDIR /app

# non-root 사용자 생성 (캐시됨)
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# 빌드된 JAR 파일만 복사
COPY --from=builder --chown=appuser:appgroup /app/service/user/build/libs/*.jar app.jar

USER appuser

# JVM 최적화 옵션 추가
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication"

# 헬스체크 (nc 대신 wget 사용 - 더 가벼움)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9000/actuator/health || exit 1

EXPOSE 9000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]