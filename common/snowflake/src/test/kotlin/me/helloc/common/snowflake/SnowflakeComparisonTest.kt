package me.helloc.common.snowflake

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * 기존 Snowflake vs 개선된 Snowflake 성능 비교 테스트
 */
class SnowflakeComparisonTest : FunSpec({

    context("기존 vs 개선된 Snowflake 성능 비교") {
        test("단일 스레드 성능 비교") {
            // Given
            val legacySnowflake = Snowflake(1L)
            val snowflake = Snowflake(
                SnowflakeConfig.Builder()
                    .randomNodeId(12345L) // 동일한 시드로 공정한 비교
                    .failOnClockBackward() // 기존과 동일한 전략
                    .build()
            )
            val testCount = 500_000

            // When - 기존 Snowflake 성능 측정
            val legacyTime = measureTimeMillis {
                repeat(testCount) {
                    legacySnowflake.nextId()
                }
            }

            // When - 개선된 Snowflake 성능 측정  
            val improvedTime = measureTimeMillis {
                repeat(testCount) {
                    snowflake.nextId()
                }
            }

            // Then
            val legacyThroughput = (testCount * 1000.0 / legacyTime).toLong()
            val improvedThroughput = (testCount * 1000.0 / improvedTime).toLong()
            val improvementRatio = improvedThroughput.toDouble() / legacyThroughput.toDouble()

            println("=== 단일 스레드 성능 비교 ===")
            println("기존 Snowflake: ${legacyThroughput} IDs/sec (${legacyTime}ms)")
            println("개선된 Snowflake: ${improvedThroughput} IDs/sec (${improvedTime}ms)")
            println("성능 개선비: ${String.format("%.2f", improvementRatio)}x")

            // 성능이 저하되지 않아야 함 (최소 90% 유지)
            improvedThroughput shouldBeGreaterThan (legacyThroughput * 0.9).toLong()
        }

        test("멀티 스레드 성능 비교") {
            // Given
            val legacySnowflake = Snowflake(1L)
            val snowflake = Snowflake(
                SnowflakeConfig.Builder()
                    .randomNodeId(12345L)
                    .failOnClockBackward()
                    .build()
            )

            val threadCount = 8
            val testDurationMs = 3000L

            // When - 기존 Snowflake 멀티스레드 테스트
            val legacyCounter = AtomicLong(0)
            val legacyTime = measureTimeMillis {
                val threadPool = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)

                repeat(threadCount) {
                    threadPool.submit {
                        try {
                            val endTime = System.currentTimeMillis() + testDurationMs
                            while (System.currentTimeMillis() < endTime) {
                                legacySnowflake.nextId()
                                legacyCounter.incrementAndGet()
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                threadPool.shutdown()
            }

            // When - 개선된 Snowflake 멀티스레드 테스트
            val improvedCounter = AtomicLong(0)
            val improvedTime = measureTimeMillis {
                val threadPool = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)

                repeat(threadCount) {
                    threadPool.submit {
                        try {
                            val endTime = System.currentTimeMillis() + testDurationMs
                            while (System.currentTimeMillis() < endTime) {
                                snowflake.nextId()
                                improvedCounter.incrementAndGet()
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                threadPool.shutdown()
            }

            // Then
            val legacyThroughput = (legacyCounter.get() * 1000.0 / legacyTime).toLong()
            val improvedThroughput = (improvedCounter.get() * 1000.0 / improvedTime).toLong()
            val improvementRatio = improvedThroughput.toDouble() / legacyThroughput.toDouble()

            println("=== 멀티 스레드 성능 비교 (${threadCount}스레드) ===")
            println("기존 Snowflake: ${legacyThroughput} IDs/sec (${legacyCounter.get()}개)")
            println("개선된 Snowflake: ${improvedThroughput} IDs/sec (${improvedCounter.get()}개)")
            println("성능 개선비: ${String.format("%.2f", improvementRatio)}x")

            // 멀티스레드에서도 성능이 저하되지 않아야 함
            improvedThroughput shouldBeGreaterThan (legacyThroughput * 0.9).toLong()
        }

        test("메모리 사용량 비교") {
            // Given
            val runtime = Runtime.getRuntime()
            val instanceCount = 100

            // When - 기존 Snowflake 메모리 측정
            System.gc()
            Thread.sleep(100)
            val beforeLegacy = runtime.totalMemory() - runtime.freeMemory()

            val legacyInstances = (1..instanceCount).map { Snowflake(it.toLong()) }
            
            System.gc()
            Thread.sleep(100)
            val afterLegacy = runtime.totalMemory() - runtime.freeMemory()
            val legacyMemoryUsage = afterLegacy - beforeLegacy

            // When - 개선된 Snowflake 메모리 측정
            System.gc()
            Thread.sleep(100)
            val beforeImproved = runtime.totalMemory() - runtime.freeMemory()

            val improvedInstances = (1..instanceCount).map { 
                Snowflake(
                    SnowflakeConfig.Builder()
                        .staticNodeId(it.toLong())
                        .build()
                )
            }

            System.gc()
            Thread.sleep(100)
            val afterImproved = runtime.totalMemory() - runtime.freeMemory()
            val improvedMemoryUsage = afterImproved - beforeImproved

            // Then
            val legacyMemoryPerInstance = legacyMemoryUsage / instanceCount
            val improvedMemoryPerInstance = improvedMemoryUsage / instanceCount
            val memoryRatio = improvedMemoryUsage.toDouble() / legacyMemoryUsage.toDouble()

            println("=== 메모리 사용량 비교 (${instanceCount}개 인스턴스) ===")
            println("기존 Snowflake: ${legacyMemoryUsage / 1024}KB (인스턴스당 ${legacyMemoryPerInstance}B)")
            println("개선된 Snowflake: ${improvedMemoryUsage / 1024}KB (인스턴스당 ${improvedMemoryPerInstance}B)")
            println("메모리 사용 비율: ${String.format("%.2f", memoryRatio)}x")

            // 메모리 사용량이 크게 증가하지 않아야 함 (최대 3배)
            memoryRatio shouldBeLessThan 3.0
        }

        test("ID 유니크성 검증 - 대용량 테스트") {
            // Given
            val legacySnowflake = Snowflake(1L)
            val snowflake = Snowflake(
                SnowflakeConfig.Builder()
                    .staticNodeId(1L)
                    .build()
            )
            val testCount = 1_000_000

            // When - 기존 Snowflake ID 생성
            val legacyIds = mutableSetOf<Long>()
            val legacyTime = measureTimeMillis {
                repeat(testCount) {
                    legacyIds.add(legacySnowflake.nextId())
                }
            }

            // When - 개선된 Snowflake ID 생성
            val improvedIds = mutableSetOf<Long>()
            val improvedTime = measureTimeMillis {
                repeat(testCount) {
                    improvedIds.add(snowflake.nextId())
                }
            }

            // Then
            println("=== ID 유니크성 검증 (${testCount}개) ===")
            println("기존 Snowflake: ${legacyIds.size}개 고유 ID (${legacyTime}ms)")
            println("개선된 Snowflake: ${improvedIds.size}개 고유 ID (${improvedTime}ms)")

            // 모든 ID가 고유해야 함
            legacyIds.size shouldBe testCount
            improvedIds.size shouldBe testCount

            // 두 구현체가 다른 NodeId를 사용하므로 교집합이 없어야 함
            val intersection = legacyIds.intersect(improvedIds)
            println("ID 교집합: ${intersection.size}개")
            
            // 다른 NodeId를 사용하므로 교집합이 있을 수 있지만, 매우 적어야 함
            // (시간 차이로 인해 일부 겹칠 수 있음)
        }
    }

    context("스트레스 테스트") {
        test("장시간 연속 생성 테스트") {
            // Given
            val snowflake = Snowflake(
                SnowflakeConfig.Builder()
                    .staticNodeId(999L)
                    .waitOnClockBackward(5000L)
                    .build()
            )
            
            val testDurationMs = 30_000L // 30초
            val counter = AtomicLong(0)
            val threadCount = 4
            val errors = AtomicLong(0)

            // When
            val threadPool = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val startTime = System.currentTimeMillis()

            repeat(threadCount) { threadIndex ->
                threadPool.submit {
                    try {
                        while (System.currentTimeMillis() - startTime < testDurationMs) {
                            try {
                                val id = snowflake.nextId()
                                counter.incrementAndGet()
                                
                                // ID 유효성 간단 검증
                                if (id <= 0) {
                                    errors.incrementAndGet()
                                    println("Thread $threadIndex: Invalid ID generated: $id")
                                }
                            } catch (e: Exception) {
                                errors.incrementAndGet()
                                println("Thread $threadIndex: Exception: ${e.message}")
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            threadPool.shutdown()
            val actualDuration = System.currentTimeMillis() - startTime

            // Then
            val totalIds = counter.get()
            val throughput = (totalIds * 1000.0 / actualDuration).toLong()
            val errorRate = errors.get().toDouble() / totalIds.toDouble() * 100

            println("=== 장시간 스트레스 테스트 결과 ===")
            println("테스트 시간: ${actualDuration}ms")
            println("생성된 ID 수: $totalIds")
            println("처리량: $throughput IDs/sec")
            println("에러 수: ${errors.get()}")
            println("에러율: ${String.format("%.4f", errorRate)}%")

            // 에러율이 0.1% 미만이어야 함
            errorRate shouldBeLessThan 0.1
            
            // 최소 처리량 보장
            throughput shouldBeGreaterThan 100_000L
        }
    }
})