package me.helloc.techwikiplus.service.user.interfaces

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HealthCheckControllerUnitTest : FunSpec({

    test("헬스체크 시 서비스 정보와 UP 상태를 반환한다") {
        // Given
        val version = "1.0.0-TEST"
        val serviceName = "techwikiplus-user"
        val controller = HealthCheckController(version, serviceName)

        // When
        val response = controller.healthCheck()

        // Then
        response.status shouldBe "UP"
        response.version shouldBe "1.0.0-TEST"
        response.serviceName shouldBe "techwikiplus-user"
    }

    test("다양한 버전과 서비스 이름으로 올바른 응답을 반환한다") {
        // Given
        val testCases =
            listOf(
                Triple("2.1.0", "user-service", "UP"),
                Triple("3.0.0-SNAPSHOT", "techwiki-user", "UP"),
                Triple("1.0.0-RC1", "auth-service", "UP"),
            )

        testCases.forEach { (version, serviceName, expectedStatus) ->
            // Given
            val controller = HealthCheckController(version, serviceName)

            // When
            val response = controller.healthCheck()

            // Then
            response.status shouldBe expectedStatus
            response.version shouldBe version
            response.serviceName shouldBe serviceName
        }
    }

    test("상태는 항상 UP을 반환한다") {
        // Given
        val controllers =
            listOf(
                HealthCheckController("1.0", "service1"),
                HealthCheckController("2.0", "service2"),
                HealthCheckController("3.0", "service3"),
            )

        controllers.forEach { controller ->
            // When
            val response = controller.healthCheck()

            // Then
            response.status shouldBe "UP"
        }
    }

    test("여러 번 호출해도 일관된 응답을 반환한다") {
        // Given
        val controller = HealthCheckController("1.2.3", "my-service")

        // When
        val responses = (1..5).map { controller.healthCheck() }

        // Then
        responses.forEach { response ->
            response.status shouldBe "UP"
            response.version shouldBe "1.2.3"
            response.serviceName shouldBe "my-service"
        }

        // 모든 응답이 동일한지 확인
        responses.all { it == responses.first() } shouldBe true
    }
})
