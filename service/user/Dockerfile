# 로컬 개발 환경용 Dockerfile
# 빠른 빌드를 위해 테스트는 CI/CD에서 수행
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# 프로젝트 파일 복사
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./
COPY common/snowflake/build.gradle.kts ./common/snowflake/
COPY service/user/build.gradle.kts ./service/user/

# 의존성 다운로드
RUN ./gradlew :service:user:dependencies --no-daemon

# 소스 코드 복사
COPY common/snowflake/src ./common/snowflake/src
COPY service/user/src ./service/user/src

# 로컬 개발 환경에서는 빠른 빌드를 위해 테스트 제외
# 테스트는 로컬에서 ./gradlew test 또는 CI/CD에서 수행
RUN ./gradlew :service:user:build -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

WORKDIR /app

# non-root 사용자 생성
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/service/user/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appgroup /app

USER appuser

# 헬스체크 추가 (포트 확인)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD nc -z localhost 9000 || exit 1

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

