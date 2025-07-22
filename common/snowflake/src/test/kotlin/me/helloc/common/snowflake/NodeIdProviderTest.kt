package me.helloc.common.snowflake

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NodeIdProviderTest : FunSpec({

    context("EnvironmentNodeIdProvider") {
        test("환경변수에서 올바른 nodeId를 가져온다") {
            // Given
            val nodeId = 123L
            val provider = EnvironmentNodeIdProvider(mapOf("SNOWFLAKE_NODE_ID" to "123"))

            // When & Then
            provider.getNodeId() shouldBe nodeId
        }

        test("환경변수가 없으면 InvalidNodeIdException 발생") {
            // Given
            val provider = EnvironmentNodeIdProvider(emptyMap())

            // When & Then
            shouldThrow<InvalidNodeIdException> {
                provider.getNodeId()
            }
        }

        test("환경변수가 숫자가 아니면 InvalidNodeIdException 발생") {
            // Given
            val provider = EnvironmentNodeIdProvider(mapOf("SNOWFLAKE_NODE_ID" to "invalid"))

            // When & Then
            shouldThrow<InvalidNodeIdException> {
                provider.getNodeId()
            }
        }

        test("nodeId가 범위를 벗어나면 InvalidNodeIdException 발생") {
            // Given
            val provider = EnvironmentNodeIdProvider(mapOf("SNOWFLAKE_NODE_ID" to "1024"))

            // When & Then
            shouldThrow<InvalidNodeIdException> {
                provider.getNodeId()
            }
        }

        test("음수 nodeId는 InvalidNodeIdException 발생") {
            // Given
            val provider = EnvironmentNodeIdProvider(mapOf("SNOWFLAKE_NODE_ID" to "-1"))

            // When & Then
            shouldThrow<InvalidNodeIdException> {
                provider.getNodeId()
            }
        }
    }

    context("RandomNodeIdProvider") {
        test("유효한 범위의 nodeId를 생성한다") {
            // Given
            val provider = RandomNodeIdProvider()

            // When
            val nodeId = provider.getNodeId()

            // Then
            nodeId shouldBe (nodeId and 1023L) // 10비트 범위 내
        }

        test("여러 번 호출해도 동일한 nodeId를 반환한다") {
            // Given
            val provider = RandomNodeIdProvider()

            // When
            val nodeId1 = provider.getNodeId()
            val nodeId2 = provider.getNodeId()

            // Then
            nodeId1 shouldBe nodeId2
        }
    }

    context("StaticNodeIdProvider") {
        test("지정된 nodeId를 반환한다") {
            // Given
            val expectedNodeId = 500L
            val provider = StaticNodeIdProvider(expectedNodeId)

            // When & Then
            provider.getNodeId() shouldBe expectedNodeId
        }

        test("잘못된 nodeId로 생성 시 예외 발생") {
            // When & Then
            shouldThrow<InvalidNodeIdException> {
                StaticNodeIdProvider(1024L) // MAX_NODE_ID 초과
            }

            shouldThrow<InvalidNodeIdException> {
                StaticNodeIdProvider(-1L) // 음수
            }
        }
    }

    context("NodeIdValidator") {
        test("유효한 nodeId 검증") {
            NodeIdValidator.validate(0L) shouldBe true
            NodeIdValidator.validate(512L) shouldBe true
            NodeIdValidator.validate(1023L) shouldBe true
        }

        test("범위 밖 nodeId 검증 실패") {
            NodeIdValidator.validate(-1L) shouldBe false
            NodeIdValidator.validate(1024L) shouldBe false
            NodeIdValidator.validate(Long.MAX_VALUE) shouldBe false
        }
    }
})
