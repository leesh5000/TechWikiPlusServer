# 🌨️ Snowflake ID Generator

분산 환경에서 고유한 64비트 ID를 안전하고 효율적으로 생성하는 Snowflake 구현체입니다.

## 📋 목차

- [특징](#특징)
- [ID 구조](#id-구조)
- [설치 및 의존성](#설치-및-의존성)
- [빠른 시작](#빠른-시작)
- [설정 가이드](#설정-가이드)
- [사용 방법](#사용-방법)
- [NodeId 관리](#nodeid-관리)
- [시계 역행 처리](#시계-역행-처리)
- [성능 및 제한사항](#성능-및-제한사항)
- [마이그레이션 가이드](#마이그레이션-가이드)
- [문제 해결](#문제-해결)

## ✨ 특징

### 🔒 **안전성**
- **분산 환경 ID 충돌 방지**: 환경변수 기반 고정 NodeId 할당
- **시계 역행 복구**: 다양한 복구 전략으로 가용성 보장
- **입력 유효성 검증**: 잘못된 설정으로 인한 오류 방지

### ⚙️ **설정 가능성**
- **유연한 NodeId 관리**: 환경변수, 고정값, 자동 할당 지원
- **복구 전략 선택**: 대기, 실패, 시퀀스 조정 전략
- **커스텀 Epoch**: 프로젝트별 시작 시점 설정

### 🚀 **성능**
- **최적화된 비트 연산**: 효율적인 ID 생성
- **메모리 효율성**: 최소한의 객체 생성
- **동시성 지원**: 멀티스레드 환경에서 안전한 ID 생성

### 🧪 **테스트 가능성**
- **TimeProvider 추상화**: 시간 의존성 제거
- **MockTimeProvider**: 테스트용 시간 제어
- **포괄적 테스트 스위트**: 43개 테스트로 검증

## 🏗️ ID 구조

64비트 ID는 다음과 같이 구성됩니다:

```
┌─────────────────────────────────────────────────┬──────────────┬──────────────────┐
│                타임스탬프 (41비트)                   │ 노드ID (10비트) │  시퀀스 (12비트)   │
├─────────────────────────────────────────────────┼──────────────┼──────────────────┤
│          Epoch 기준 밀리초 (2^41 ≈ 69년)           │   0 ~ 1023   │    0 ~ 4095     │
└─────────────────────────────────────────────────┴──────────────┴──────────────────┘
```

- **타임스탬프**: Epoch(기본값: 2024-01-01)부터의 밀리초
- **노드ID**: 분산 환경에서 각 인스턴스를 구분하는 고유 식별자
- **시퀀스**: 동일 밀리초 내에서 생성된 ID를 구분하는 일련번호

## 📦 설치 및 의존성

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation(project(":common:snowflake"))
}
```

### 필요한 Kotlin 버전
- Kotlin 1.9.25 이상
- JDK 21 이상

## 🚀 빠른 시작

### 1. 기본 사용법

```kotlin
// 환경변수에 SNOWFLAKE_NODE_ID 설정 필요
val snowflake = Snowflake(SnowflakeConfig())
val id = snowflake.nextId()
println("Generated ID: $id")
```

### 2. 고정 NodeId 사용

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)
    .build()

val snowflake = Snowflake(config)
val id = snowflake.nextId()
```

### 3. 시계 역행 대기 전략

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(456L)
    .waitOnClockBackward(maxWaitTimeMillis = 5000L)
    .build()

val snowflake = Snowflake(config)
```

## ⚙️ 설정 가이드

### Builder 패턴으로 설정

```kotlin
val config = SnowflakeConfig.Builder()
    // NodeId 설정 (필수)
    .staticNodeId(100L)                    // 고정 NodeId
    // 또는
    .environmentNodeId()                   // 환경변수에서 읽기
    // 또는  
    .randomNodeId(seed = 12345L)          // 랜덤 NodeId (테스트용)
    
    // Epoch 설정 (선택)
    .epochMillis(1640995200000L)          // 커스텀 시작 시점
    
    // 시계 역행 처리 (선택)
    .waitOnClockBackward(3000L)           // 대기 전략
    // 또는
    .failOnClockBackward()                // 실패 전략 (기본값)
    // 또는
    .useSequenceOnClockBackward()         // 시퀀스 전략
    
    .build()

val snowflake = Snowflake(config)
```

### 환경변수 설정

```bash
# 기본 환경변수명
export SNOWFLAKE_NODE_ID=123

# 커스텀 환경변수명 사용
val config = SnowflakeConfig.Builder()
    .environmentNodeId(envKey = "MY_NODE_ID")
    .build()
```

## 💡 사용 방법

### ID 생성

```kotlin
val snowflake = Snowflake(config)

// 단일 ID 생성
val id = snowflake.nextId()

// 여러 ID 생성
val ids = (1..1000).map { snowflake.nextId() }
```

### 멀티스레드 환경

```kotlin
val snowflake = Snowflake(config)
val threadPool = Executors.newFixedThreadPool(10)

repeat(10) {
    threadPool.submit {
        repeat(1000) {
            val id = snowflake.nextId()
            println("Thread ${Thread.currentThread().id}: $id")
        }
    }
}
```

### 인스턴스 정보 확인

```kotlin
val info = snowflake.getInfo()
println("NodeId: ${info.nodeId}")
println("Epoch: ${info.epochMillis}")
println("Last Generated: ${info.lastGeneratedTimeMillis}")
println("Current Sequence: ${info.currentSequence}")
println("Time since last generation: ${info.timeSinceLastGeneration()}ms")
```

## 🏷️ NodeId 관리

### 1. 환경변수 기반 (권장 - 프로덕션)

```kotlin
// 환경변수 설정
export SNOWFLAKE_NODE_ID=123

// 코드
val config = SnowflakeConfig.Builder()
    .environmentNodeId()
    .build()
```

**장점:**
- 배포 환경별 다른 NodeId 할당 가능
- 코드 변경 없이 NodeId 조정
- 분산 환경에서 충돌 방지

### 2. 고정값 (개발/테스트)

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(456L)
    .build()
```

**사용 시기:**
- 단일 인스턴스 환경
- 개발 및 테스트 환경
- NodeId를 명시적으로 제어해야 하는 경우

### 3. 랜덤 (레거시 호환)

```kotlin
val config = SnowflakeConfig.Builder()
    .randomNodeId(seed = 12345L)  // seed는 선택사항
    .build()
```

**⚠️ 주의:**
- 분산 환경에서 ID 충돌 위험
- 테스트나 마이그레이션 용도로만 사용

## ⏰ 시계 역행 처리

시스템 시계가 역행하는 상황에 대한 3가지 처리 전략을 제공합니다.

### 1. 대기 전략 (WaitStrategy)

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)
    .waitOnClockBackward(maxWaitTimeMillis = 5000L)
    .build()
```

**동작:**
- 시계가 정상으로 돌아올 때까지 대기
- 최대 대기 시간 초과 시 `ClockBackwardException` 발생

**사용 시기:**
- 일시적인 시계 조정이 예상되는 환경
- 가용성을 최우선으로 하는 서비스

### 2. 실패 전략 (FailStrategy) - 기본값

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)
    .failOnClockBackward()
    .build()
```

**동작:**
- 시계 역행 감지 시 즉시 `ClockBackwardException` 발생

**사용 시기:**
- 데이터 일관성이 중요한 서비스
- 시계 역행이 발생하지 않는 안정적인 환경

### 3. 시퀀스 전략 (SequenceStrategy)

```kotlin
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)
    .useSequenceOnClockBackward()
    .build()
```

**동작:**
- 마지막 시간을 유지하고 시퀀스로 고유성 보장

**사용 시기:**
- ID 생성을 중단할 수 없는 중요한 서비스
- 시퀀스 소모가 문제가 되지 않는 환경

## 📊 성능 및 제한사항

### 성능 특성

- **초당 최대 ID**: 4,096,000개 (단일 노드 기준)
- **최대 노드 수**: 1,024개
- **사용 가능 기간**: 약 69년 (2024년 기준)
- **메모리 사용량**: 인스턴스당 최소

### 제한사항

1. **시퀀스 오버플로우**
   ```kotlin
   // 동일 밀리초에 4,096개 이상 생성 시 다음 밀리초까지 대기
   repeat(4097) { snowflake.nextId() } // 마지막 ID는 다음 밀리초에 생성
   ```

2. **NodeId 범위**
   ```kotlin
   // 0-1023 범위를 벗어나면 InvalidNodeIdException 발생
   StaticNodeIdProvider(1024L) // ❌ Exception
   StaticNodeIdProvider(1023L) // ✅ OK
   ```

3. **시계 동기화**
   - NTP 동기화 필수
   - 시계 역행 시 복구 전략 필요

## 💡 기본 사용법

### 간단한 사용
```kotlin
// 가장 간단한 사용법 (환경변수에 SNOWFLAKE_NODE_ID 필요)
val snowflake = Snowflake()
val id = snowflake.nextId()
```

### 고정 NodeId 사용
```kotlin
// 고정 NodeId로 간편 생성
val snowflake = Snowflake(123L)
val id = snowflake.nextId()
```

### 상세 설정
```kotlin
// 세부 설정이 필요한 경우
val config = SnowflakeConfig.Builder()
    .environmentNodeId() // 또는 .staticNodeId(123L)
    .waitOnClockBackward() // 가용성 향상
    .build()
val snowflake = Snowflake(config)
```

## 🔧 문제 해결

### 자주 발생하는 문제

#### 1. InvalidNodeIdException
```
Exception: NodeId 1024 is out of range (0-1023)
```

**해결방법:**
```kotlin
// ❌ 잘못된 NodeId
.staticNodeId(1024L)

// ✅ 올바른 NodeId  
.staticNodeId(1023L) // 또는 0-1023 범위의 값
```

#### 2. 환경변수 누락
```
Exception: Environment variable 'SNOWFLAKE_NODE_ID' not found
```

**해결방법:**
```bash
# 환경변수 설정
export SNOWFLAKE_NODE_ID=123

# 또는 다른 NodeId 전략 사용
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)  // 환경변수 대신 고정값 사용
    .build()
```

#### 3. ClockBackwardException
```
Exception: Clock moved backwards: last=1609459261000, current=1609459260000, diff=1000ms
```

**해결방법:**
```kotlin
// 대기 전략으로 변경
val config = SnowflakeConfig.Builder()
    .staticNodeId(123L)
    .waitOnClockBackward(5000L) // 5초까지 대기
    .build()
```

### 디버깅 정보

```kotlin
// 인스턴스 정보 확인
val info = snowflake.getInfo()
println("NodeId: ${info.nodeId}")
println("마지막 생성 시간: ${info.lastGeneratedTimeMillis}")
println("현재 시퀀스: ${info.currentSequence}")

// ID 구조 분석
fun analyzeId(id: Long) {
    val timestamp = (id shr 22)
    val nodeId = (id shr 12) and 1023L
    val sequence = id and 4095L
    
    println("ID: $id")
    println("  타임스탬프: $timestamp")
    println("  노드ID: $nodeId")  
    println("  시퀀스: $sequence")
}
```

### 모니터링

```kotlin
// 성능 측정
val startTime = System.nanoTime()
repeat(10000) { snowflake.nextId() }
val endTime = System.nanoTime()
val throughput = 10000 * 1_000_000_000L / (endTime - startTime)
println("처리량: ${throughput} IDs/sec")

// 시퀀스 사용량 모니터링
val info = snowflake.getInfo()
if (info.currentSequence > 3000) {
    logger.warn("높은 시퀀스 사용량: ${info.currentSequence}/4095")
}
```

## 📚 추가 자료

- [Twitter Snowflake 원본 논문](https://blog.twitter.com/engineering/en_us/a/2010/announcing-snowflake)
- [분산 ID 생성 전략 비교](https://instagram-engineering.com/sharding-ids-at-instagram-1cf5a71e5a5c)
- [시계 동기화 모범 사례](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/set-time.html)

## 🤝 기여하기

1. 이슈 생성 또는 기능 제안
2. 포크 및 브랜치 생성
3. 테스트와 함께 코드 작성
4. Pull Request 생성

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.