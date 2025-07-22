package me.helloc.common.snowflake

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SnowflakeTest : FunSpec({

    context("Snowflake 기본 기능") {
        test("설정을 사용해 Snowflake 생성") {
            // Given
            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(123L)
                    .build()

            // When
            val snowflake = Snowflake(config)

            // Then
            snowflake.nodeId shouldBe 123L
        }

        test("기본 생성자로 Snowflake 생성") {
            // Given
            val environment = mapOf("SNOWFLAKE_NODE_ID" to "456")
            val config =
                SnowflakeConfig.Builder()
                    .environmentNodeId(environment)
                    .build()

            // When
            val snowflake = Snowflake(config)

            // Then
            snowflake.nodeId shouldBe 456L
        }

        test("NodeId 지정으로 Snowflake 생성") {
            // When
            val snowflake = Snowflake(789L)

            // Then
            snowflake.nodeId shouldBe 789L
        }

        test("Companion object factory 메서드") {
            // When
            val snowflake1 = Snowflake.create(100L)
            val snowflake2 = Snowflake.create(200L)

            // Then
            snowflake1.nodeId shouldBe 100L
            snowflake2.nodeId shouldBe 200L
        }

        test("연속된 ID는 오름차순이다") {
            // Given
            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id1 = snowflake.nextId()
            val id2 = snowflake.nextId()
            val id3 = snowflake.nextId()

            // Then
            id2 shouldBeGreaterThan id1
            id3 shouldBeGreaterThan id2
        }

        test("동일한 밀리초 내에서 시퀀스가 증가한다") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            mockTimeProvider.setRepeatingTime(1000L)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id1 = snowflake.nextId()
            val id2 = snowflake.nextId()

            // Then
            val sequence1 = id1 and SnowflakeConfig.MAX_SEQUENCE
            val sequence2 = id2 and SnowflakeConfig.MAX_SEQUENCE
            sequence2 shouldBe sequence1 + 1
        }

        test("다른 밀리초에서는 시퀀스가 초기화된다") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            mockTimeProvider.setTimes(1000L, 1001L)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id1 = snowflake.nextId()
            val id2 = snowflake.nextId()

            // Then
            val sequence1 = id1 and SnowflakeConfig.MAX_SEQUENCE
            val sequence2 = id2 and SnowflakeConfig.MAX_SEQUENCE
            sequence1 shouldBe 0L
            sequence2 shouldBe 0L
        }
    }

    context("시계 역행 처리") {
        test("WaitStrategy로 시계 역행 복구") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            // 정상 시간 → 역행 → 정상 복구
            mockTimeProvider.setTimes(1000L, 900L, 900L, 1001L)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .waitOnClockBackward(1000L)
                    .build()
            val snowflake = Snowflake(config)

            // When - 첫 번째 호출 후 시계가 역행하지만 복구됨
            val id1 = snowflake.nextId()
            val id2 = snowflake.nextId()

            // Then
            id2 shouldBeGreaterThan id1
        }

        test("FailStrategy로 시계 역행 시 즉시 실패") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            mockTimeProvider.setTimes(1000L, 900L)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .failOnClockBackward()
                    .build()
            val snowflake = Snowflake(config)

            // When
            snowflake.nextId() // 첫 번째는 성공

            // Then
            shouldThrow<ClockBackwardException> {
                snowflake.nextId() // 두 번째에서 실패
            }
        }
    }

    context("동시성 테스트") {
        test("멀티스레드 환경에서 고유 ID 생성") {
            // Given
            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .build()
            val snowflake = Snowflake(config)

            val threadCount = 10
            val idsPerThread = 1000
            val threadPool = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val allIds = mutableSetOf<Long>()
            val lock = Any()

            // When
            repeat(threadCount) {
                threadPool.submit {
                    try {
                        val threadIds = mutableListOf<Long>()
                        repeat(idsPerThread) {
                            threadIds.add(snowflake.nextId())
                        }

                        synchronized(lock) {
                            allIds.addAll(threadIds)
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            threadPool.shutdown()

            // Then
            val expectedCount = threadCount * idsPerThread
            allIds.size shouldBe expectedCount // 모든 ID가 고유함
        }
    }

    context("ID 구조 분석") {
        test("생성된 ID에서 타임스탬프 추출") {
            // Given
            val fixedTime = 1000000L
            val mockTimeProvider = MockTimeProvider()
            mockTimeProvider.setRepeatingTime(fixedTime)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(123L)
                    .epochMillis(0L)
                    .timeProvider(mockTimeProvider)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id = snowflake.nextId()

            // Then
            val extractedTimestamp = (id shr 22) // 22비트 오른쪽 시프트
            extractedTimestamp shouldBe fixedTime
        }

        test("생성된 ID에서 노드ID 추출") {
            // Given
            val expectedNodeId = 123L
            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(expectedNodeId)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id = snowflake.nextId()

            // Then
            val extractedNodeId = (id shr 12) and SnowflakeConfig.MAX_NODE_ID
            extractedNodeId shouldBe expectedNodeId
        }

        test("생성된 ID에서 시퀀스 추출") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            mockTimeProvider.setRepeatingTime(1000L)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .build()
            val snowflake = Snowflake(config)

            // When
            val id1 = snowflake.nextId()
            val id2 = snowflake.nextId()
            val id3 = snowflake.nextId()

            // Then
            val sequence1 = id1 and SnowflakeConfig.MAX_SEQUENCE
            val sequence2 = id2 and SnowflakeConfig.MAX_SEQUENCE
            val sequence3 = id3 and SnowflakeConfig.MAX_SEQUENCE

            sequence1 shouldBe 0L
            sequence2 shouldBe 1L
            sequence3 shouldBe 2L
        }
    }

    context("시퀀스 오버플로우") {
        test("시퀀스 최대값 도달 시 다음 밀리초 대기") {
            // Given
            val mockTimeProvider = MockTimeProvider()
            // 같은 시간이 계속 반환되다가 마지막에 다음 시간 반환
            val sameTimes = LongArray(SnowflakeConfig.MAX_SEQUENCE.toInt() + 2) { 1000L }
            sameTimes[sameTimes.size - 1] = 1001L
            mockTimeProvider.setTimes(*sameTimes)

            val config =
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .timeProvider(mockTimeProvider)
                    .build()
            val snowflake = Snowflake(config)

            // When - 시퀀스를 최대값까지 채움
            repeat(SnowflakeConfig.MAX_SEQUENCE.toInt() + 1) {
                snowflake.nextId()
            }

            // 마지막 ID는 다음 밀리초에서 생성되어야 함
            val lastId = snowflake.nextId()

            // Then
            val timestamp = (lastId shr 22)
            val sequence = lastId and SnowflakeConfig.MAX_SEQUENCE
            timestamp shouldBe 1001L - config.epochMillis
            sequence shouldBe 0L
        }
    }
})
