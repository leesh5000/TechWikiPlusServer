package me.helloc.common.snowflake

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Snowflake 성능 테스트
 * - 1초 이내 100만개 ID 생성 테스트
 * - 메모리 사용량 테스트
 * - 처리량 측정
 * - 다양한 동시성 레벨 테스트
 */
class SnowflakePerformanceTest : FunSpec({

    context("100만개 ID 생성 성능 테스트") {
        test("1초 이내에 100만개 ID 생성 - 단일 스레드") {
            // Given
            val config = SnowflakeConfig.Builder()
                .staticNodeId(1L)
                .build()
            val snowflake = Snowflake(config)
            val targetCount = 1_000_000
            val targetTimeMs = 1000L

            // When
            val elapsedTime = measureTimeMillis {
                repeat(targetCount) {
                    snowflake.nextId()
                }
            }

            // Then
            println("100만개 ID 생성 시간: ${elapsedTime}ms")
            println("처리량: ${(targetCount * 1000.0 / elapsedTime).toLong()} IDs/sec")
            
            elapsedTime shouldBeLessThan targetTimeMs
        }

        test("1초 이내에 100만개 ID 생성 - 멀티 스레드") {
            // Given
            val config = SnowflakeConfig.Builder()
                .staticNodeId(2L)
                .build()
            val snowflake = Snowflake(config)
            val targetCount = 1_000_000
            val targetTimeMs = 1000L
            val threadCount = Runtime.getRuntime().availableProcessors()
            val idsPerThread = targetCount / threadCount

            val threadPool = Executors.newFixedThreadPool(threadCount)
            val generatedIds = mutableSetOf<Long>()
            val lock = Any()

            // When
            val elapsedTime = measureTimeMillis {
                val futures = (1..threadCount).map {
                    CompletableFuture.supplyAsync({
                        val threadIds = mutableListOf<Long>()
                        repeat(idsPerThread) {
                            threadIds.add(snowflake.nextId())
                        }
                        threadIds
                    }, threadPool)
                }

                futures.forEach { future ->
                    val threadIds = future.get()
                    synchronized(lock) {
                        generatedIds.addAll(threadIds)
                    }
                }
            }

            threadPool.shutdown()

            // Then
            val actualCount = generatedIds.size
            println("실제 생성된 ID 수: $actualCount")
            println("멀티스레드 생성 시간: ${elapsedTime}ms")
            println("처리량: ${(actualCount * 1000.0 / elapsedTime).toLong()} IDs/sec")
            println("스레드 수: $threadCount")
            
            elapsedTime shouldBeLessThan targetTimeMs
            actualCount shouldBe (threadCount * idsPerThread)
            generatedIds.size shouldBe actualCount // 모든 ID가 고유함
        }

        test("최대 처리량 측정 - 10초간 생성") {
            // Given
            val config = SnowflakeConfig.Builder()
                .staticNodeId(3L)
                .build()
            val snowflake = Snowflake(config)
            val testDurationMs = 10_000L
            val counter = AtomicLong(0)
            val threadCount = Runtime.getRuntime().availableProcessors()
            
            val threadPool = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // When
            val startTime = System.currentTimeMillis()
            repeat(threadCount) {
                threadPool.submit {
                    try {
                        while (System.currentTimeMillis() - startTime < testDurationMs) {
                            snowflake.nextId()
                            counter.incrementAndGet()
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
            
            println("=== 최대 처리량 테스트 결과 ===")
            println("테스트 시간: ${actualDuration}ms")
            println("생성된 ID 수: $totalIds")
            println("처리량: $throughput IDs/sec")
            println("스레드 수: $threadCount")
            
            // 최소 성능 기준: 100만 IDs/sec
            throughput shouldBeGreaterThan 1_000_000L
        }
    }

    context("메모리 사용량 테스트") {
        test("대량 ID 생성 시 메모리 누수 확인") {
            // Given
            val config = SnowflakeConfig.Builder()
                .staticNodeId(4L)
                .build()
            val snowflake = Snowflake(config)
            
            val runtime = Runtime.getRuntime()
            val iterations = 10
            val idsPerIteration = 100_000

            // When & Then
            val memoryUsages = mutableListOf<Long>()
            
            repeat(iterations) { iteration ->
                // GC 실행하여 이전 메모리 정리
                System.gc()
                Thread.sleep(100) // GC 완료 대기
                
                val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
                
                // ID 생성 (지역 변수로 생성하여 GC 대상이 되도록)
                repeat(idsPerIteration) {
                    snowflake.nextId()
                }
                
                System.gc()
                Thread.sleep(100)
                
                val afterMemory = runtime.totalMemory() - runtime.freeMemory()
                val memoryDiff = afterMemory - beforeMemory
                memoryUsages.add(memoryDiff)
                
                println("반복 ${iteration + 1}: 메모리 증가 = ${memoryDiff / 1024}KB")
            }
            
            // 메모리 누수 확인 - 후반부 평균이 전반부 평균보다 크게 증가하지 않아야 함
            val firstHalf = memoryUsages.take(iterations / 2).average()
            val secondHalf = memoryUsages.takeLast(iterations / 2).average()
            val memoryIncreaseRatio = secondHalf / firstHalf
            
            println("전반부 평균 메모리 증가: ${firstHalf / 1024}KB")
            println("후반부 평균 메모리 증가: ${secondHalf / 1024}KB")
            println("메모리 증가 비율: ${String.format("%.2f", memoryIncreaseRatio)}")
            
            // 메모리 증가 비율이 2배를 넘지 않아야 함 (메모리 누수 없음)
            memoryIncreaseRatio shouldBeLessThan 2.0
        }

        test("인스턴스 메모리 사용량 측정") {
            // Given
            val runtime = Runtime.getRuntime()
            System.gc()
            Thread.sleep(100)
            
            val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // When - 여러 인스턴스 생성
            val instances = (1..1000).map { nodeId ->
                Snowflake(
                    SnowflakeConfig.Builder()
                        .staticNodeId(nodeId.toLong())
                        .build()
                )
            }
            
            System.gc()
            Thread.sleep(100)
            
            val afterMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // Then
            val memoryPerInstance = (afterMemory - beforeMemory) / instances.size
            println("인스턴스 1000개 생성:")
            println("총 메모리 사용량: ${(afterMemory - beforeMemory) / 1024}KB")
            println("인스턴스당 메모리: ${memoryPerInstance}B")
            
            // 각 인스턴스가 1KB 미만의 메모리를 사용해야 함
            memoryPerInstance shouldBeLessThan 1024L
        }
    }

    context("동시성 스케일링 테스트") {
        test("스레드 수에 따른 성능 변화") {
            // Given
            val config = SnowflakeConfig.Builder()
                .staticNodeId(5L)
                .build()
            val snowflake = Snowflake(config)
            val testDurationMs = 2000L
            val threadCounts = listOf(1, 2, 4, 8, 16, 32)
            
            threadCounts.forEach { threadCount ->
                val counter = AtomicLong(0)
                val threadPool = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                
                // When
                val startTime = System.currentTimeMillis()
                repeat(threadCount) {
                    threadPool.submit {
                        try {
                            while (System.currentTimeMillis() - startTime < testDurationMs) {
                                snowflake.nextId()
                                counter.incrementAndGet()
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
                
                println("스레드 $threadCount: $throughput IDs/sec")
            }
        }
    }

    context("NodeId 전략별 성능 비교") {
        test("다른 NodeId 전략의 성능 차이") {
            val testCount = 100_000
            val strategies = mapOf(
                "Static NodeId" to SnowflakeConfig.Builder().staticNodeId(1L).build(),
                "Environment NodeId" to SnowflakeConfig.Builder()
                    .environmentNodeId(mapOf("SNOWFLAKE_NODE_ID" to "2")).build(),
                "Random NodeId" to SnowflakeConfig.Builder().randomNodeId(12345L).build()
            )
            
            strategies.forEach { (name, config) ->
                val snowflake = Snowflake(config)
                
                val elapsedTime = measureTimeMillis {
                    repeat(testCount) {
                        snowflake.nextId()
                    }
                }
                
                val throughput = (testCount * 1000.0 / elapsedTime).toLong()
                println("$name: $throughput IDs/sec")
            }
        }
    }

    context("시계 역행 전략별 성능 비교") {
        test("정상 상황에서 시계 역행 전략 성능 차이") {
            val testCount = 100_000
            val strategies = mapOf(
                "Fail Strategy" to SnowflakeConfig.Builder().staticNodeId(1L).failOnClockBackward().build(),
                "Wait Strategy" to SnowflakeConfig.Builder().staticNodeId(2L).waitOnClockBackward(1000L).build(),
                "Sequence Strategy" to SnowflakeConfig.Builder().staticNodeId(3L).useSequenceOnClockBackward().build()
            )
            
            strategies.forEach { (name, config) ->
                val snowflake = Snowflake(config)
                
                val elapsedTime = measureTimeMillis {
                    repeat(testCount) {
                        snowflake.nextId()
                    }
                }
                
                val throughput = (testCount * 1000.0 / elapsedTime).toLong()
                println("$name: $throughput IDs/sec")
            }
        }
    }
})