package me.helloc.common.snowflake

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 * 빠른 성능 확인을 위한 테스트
 */
class QuickPerformanceTest : FunSpec({

    test("1 million IDs generation - single thread") {
        // Given
        val config =
            SnowflakeConfig.Builder()
                .staticNodeId(1L)
                .build()
        val snowflake = Snowflake(config)
        val targetCount = 1_000_000
        val targetTimeMs = 1000L

        // When
        val elapsedTime =
            measureTimeMillis {
                repeat(targetCount) {
                    snowflake.nextId()
                }
            }

        // Then
        val throughput = (targetCount * 1000.0 / elapsedTime).toLong()

        println("=== Single Thread Performance ===")
        println("Generated IDs: $targetCount")
        println("Time taken: ${elapsedTime}ms")
        println("Throughput: $throughput IDs/sec")
        println("Target: Generate 1M IDs in less than 1 second")
        println("Result: ${if (elapsedTime < targetTimeMs) "✅ PASSED" else "❌ FAILED"}")

        elapsedTime shouldBeLessThan targetTimeMs
    }

    test("1 million IDs generation - multi thread") {
        // Given
        val config =
            SnowflakeConfig.Builder()
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
        val elapsedTime =
            measureTimeMillis {
                val futures =
                    (1..threadCount).map {
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
        val throughput = (actualCount * 1000.0 / elapsedTime).toLong()

        println("=== Multi Thread Performance ===")
        println("Generated IDs: $actualCount")
        println("Time taken: ${elapsedTime}ms")
        println("Throughput: $throughput IDs/sec")
        println("Thread count: $threadCount")
        println("Target: Generate 1M IDs in less than 1 second")
        println("Result: ${if (elapsedTime < targetTimeMs) "✅ PASSED" else "❌ FAILED"}")

        elapsedTime shouldBeLessThan targetTimeMs
        actualCount shouldBe (threadCount * idsPerThread)
        generatedIds.size shouldBe actualCount // All IDs are unique
    }

    test("Maximum throughput measurement") {
        // Given
        val config =
            SnowflakeConfig.Builder()
                .staticNodeId(3L)
                .build()
        val snowflake = Snowflake(config)
        val testDurationMs = 5000L
        val threadCount = Runtime.getRuntime().availableProcessors()

        val threadPool = Executors.newFixedThreadPool(threadCount)
        var totalIds = 0L

        // When
        val elapsedTime =
            measureTimeMillis {
                val futures =
                    (1..threadCount).map {
                        CompletableFuture.supplyAsync({
                            var count = 0L
                            val startTime = System.currentTimeMillis()
                            while (System.currentTimeMillis() - startTime < testDurationMs) {
                                snowflake.nextId()
                                count++
                            }
                            count
                        }, threadPool)
                    }

                totalIds = futures.sumOf { it.get() }
            }

        threadPool.shutdown()

        // Then
        val throughput = (totalIds * 1000.0 / elapsedTime).toLong()

        println("=== Maximum Throughput Test ===")
        println("Test duration: ${elapsedTime}ms")
        println("Generated IDs: $totalIds")
        println("Throughput: $throughput IDs/sec")
        println("Thread count: $threadCount")
        println("Target: At least 1M IDs/sec")
        println("Result: ${if (throughput >= 1_000_000L) "✅ PASSED" else "❌ FAILED"}")

        // Should achieve at least 1M IDs per second
        throughput shouldBeGreaterThan 1_000_000L
    }
})
