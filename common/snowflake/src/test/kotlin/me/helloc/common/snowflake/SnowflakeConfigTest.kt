package me.helloc.common.snowflake

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SnowflakeConfigTest : FunSpec({

    context("SnowflakeConfig 생성") {
        test("기본값으로 생성") {
            // When
            val config = SnowflakeConfig()

            // Then
            config.epochMillis shouldBe SnowflakeConfig.DEFAULT_EPOCH
            config.clockBackwardStrategy.shouldBeInstanceOf<WaitStrategy>()
            config.timeProvider.shouldBeInstanceOf<SystemTimeProvider>()
        }

        test("커스텀 값으로 생성") {
            // Given
            val customEpoch = 1640995200000L // 2022-01-01
            val nodeIdProvider = StaticNodeIdProvider(123L)
            val clockStrategy = FailStrategy()

            // When
            val config = SnowflakeConfig(
                nodeIdProvider = nodeIdProvider,
                epochMillis = customEpoch,
                clockBackwardStrategy = clockStrategy
            )

            // Then
            config.nodeIdProvider shouldBe nodeIdProvider
            config.epochMillis shouldBe customEpoch
            config.clockBackwardStrategy shouldBe clockStrategy
        }

        test("잘못된 epoch 시간으로 생성 시 예외 발생") {
            // Given
            val futureEpoch = System.currentTimeMillis() + 86400000L // 미래 시간

            // When & Then
            shouldThrow<IllegalArgumentException> {
                SnowflakeConfig(epochMillis = futureEpoch)
            }
        }

        test("음수 epoch 시간으로 생성 시 예외 발생") {
            // When & Then
            shouldThrow<IllegalArgumentException> {
                SnowflakeConfig(epochMillis = -1L)
            }
        }
    }

    context("SnowflakeConfig.Builder") {
        test("빌더 패턴으로 설정 생성") {
            // Given
            val nodeIdProvider = StaticNodeIdProvider(456L)
            val clockStrategy = WaitStrategy(3000L)

            // When
            val config = SnowflakeConfig.Builder()
                .nodeIdProvider(nodeIdProvider)
                .epochMillis(1609459200000L) // 2021-01-01
                .clockBackwardStrategy(clockStrategy)
                .build()

            // Then
            config.nodeIdProvider shouldBe nodeIdProvider
            config.epochMillis shouldBe 1609459200000L
            config.clockBackwardStrategy shouldBe clockStrategy
        }

        test("환경변수 기반 nodeId 설정") {
            // Given
            val environment = mapOf("SNOWFLAKE_NODE_ID" to "789")

            // When
            val config = SnowflakeConfig.Builder()
                .environmentNodeId(environment)
                .build()

            // Then
            config.nodeIdProvider.shouldBeInstanceOf<EnvironmentNodeIdProvider>()
            config.nodeIdProvider.getNodeId() shouldBe 789L
        }

        test("정적 nodeId 설정") {
            // When
            val config = SnowflakeConfig.Builder()
                .staticNodeId(123L)
                .build()

            // Then
            config.nodeIdProvider.shouldBeInstanceOf<StaticNodeIdProvider>()
            config.nodeIdProvider.getNodeId() shouldBe 123L
        }

        test("랜덤 nodeId 설정") {
            // When
            val config = SnowflakeConfig.Builder()
                .randomNodeId(12345L) // seed
                .build()

            // Then
            config.nodeIdProvider.shouldBeInstanceOf<RandomNodeIdProvider>()
            val nodeId = config.nodeIdProvider.getNodeId()
            nodeId shouldBe (nodeId and 1023L) // 유효한 범위
        }

        test("대기 전략 설정") {
            // When
            val config = SnowflakeConfig.Builder()
                .waitOnClockBackward(2000L)
                .build()

            // Then
            config.clockBackwardStrategy.shouldBeInstanceOf<WaitStrategy>()
        }

        test("실패 전략 설정") {
            // When
            val config = SnowflakeConfig.Builder()
                .failOnClockBackward()
                .build()

            // Then
            config.clockBackwardStrategy.shouldBeInstanceOf<FailStrategy>()
        }

        test("시퀀스 전략 설정") {
            // When
            val config = SnowflakeConfig.Builder()
                .useSequenceOnClockBackward()
                .build()

            // Then
            config.clockBackwardStrategy.shouldBeInstanceOf<SequenceStrategy>()
        }
    }

    context("기본 설정 검증") {
        test("DEFAULT_EPOCH은 2024년 1월 1일이다") {
            // Then
            SnowflakeConfig.DEFAULT_EPOCH shouldBe 1704067200000L
        }

        test("기본 NodeIdProvider는 환경변수 기반이다") {
            // When
            val config = SnowflakeConfig()

            // Then
            config.nodeIdProvider.shouldBeInstanceOf<EnvironmentNodeIdProvider>()
        }
    }
})