package me.helloc.common.snowflake

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * JMH 기반 Snowflake 성능 벤치마크 테스트
 *
 * 실행 방법:
 * ./gradlew :common:snowflake:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
open class SnowflakeBenchmarkTest {

    private lateinit var snowflakeEnvironment: Snowflake
    private lateinit var snowflakeStatic: Snowflake
    private lateinit var snowflakeRandom: Snowflake
    private lateinit var snowflakeWaitStrategy: Snowflake

    @Setup
    fun setup() {
        // 환경변수 기반 Snowflake
        snowflakeEnvironment = Snowflake(
            SnowflakeConfig.Builder()
                .environmentNodeId(mapOf("SNOWFLAKE_NODE_ID" to "100"))
                .failOnClockBackward()
                .build()
        )

        // 고정 NodeId Snowflake
        snowflakeStatic = Snowflake(200L)

        // 랜덤 NodeId Snowflake (시드 고정으로 재현 가능)
        snowflakeRandom = Snowflake(
            SnowflakeConfig.Builder()
                .randomNodeId(12345L)
                .failOnClockBackward()
                .build()
        )

        // 대기 전략 Snowflake
        snowflakeWaitStrategy = Snowflake(
            SnowflakeConfig.Builder()
                .staticNodeId(300L)
                .waitOnClockBackward(1000L)
                .build()
        )
    }

    @Benchmark
    fun snowflake_environmentNodeId(blackhole: Blackhole) {
        blackhole.consume(snowflakeEnvironment.nextId())
    }

    @Benchmark
    fun snowflake_staticNodeId(blackhole: Blackhole) {
        blackhole.consume(snowflakeStatic.nextId())
    }

    @Benchmark
    fun snowflake_randomNodeId(blackhole: Blackhole) {
        blackhole.consume(snowflakeRandom.nextId())
    }

    @Benchmark
    fun snowflake_waitStrategy(blackhole: Blackhole) {
        blackhole.consume(snowflakeWaitStrategy.nextId())
    }
}

/**
 * 멀티스레드 성능 벤치마크
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
open class SnowflakeMultiThreadBenchmarkTest {

    private lateinit var snowflake: Snowflake

    @Setup
    fun setup() {
        snowflake = Snowflake(100L)
    }

    @Benchmark
    @Threads(1)
    fun singleThread(blackhole: Blackhole) {
        blackhole.consume(snowflake.nextId())
    }

    @Benchmark
    @Threads(2)
    fun twoThreads(blackhole: Blackhole) {
        blackhole.consume(snowflake.nextId())
    }

    @Benchmark
    @Threads(4)
    fun fourThreads(blackhole: Blackhole) {
        blackhole.consume(snowflake.nextId())
    }

    @Benchmark
    @Threads(8)
    fun eightThreads(blackhole: Blackhole) {
        blackhole.consume(snowflake.nextId())
    }

    @Benchmark
    @Threads(16)
    fun sixteenThreads(blackhole: Blackhole) {
        blackhole.consume(snowflake.nextId())
    }
}
