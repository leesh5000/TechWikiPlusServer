package me.helloc.techwikiplus.service.user.interfaces

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.interfaces.UserVerifyController.Request
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserVerifyUseCase
import org.springframework.http.HttpStatus

class UserVerifyControllerUnitTest : FunSpec({

    // Fake implementation for testing
    class FakeUserVerifyUseCase : UserVerifyUseCase {
        var verifyCalled = false
        var lastEmail: Email? = null
        var lastCode: VerificationCode? = null
        var shouldThrowException: Exception? = null

        override fun execute(command: UserVerifyUseCase.Command) {
            verifyCalled = true
            lastEmail = command.email
            lastCode = command.code

            shouldThrowException?.let { throw it }
        }

        fun reset() {
            verifyCalled = false
            lastEmail = null
            lastCode = null
            shouldThrowException = null
        }
    }

    val fakeUseCase = FakeUserVerifyUseCase()
    val controller = UserVerifyController(fakeUseCase)

    beforeEach {
        fakeUseCase.reset()
    }

    test("인증 요청 시 UseCase를 호출하고 201 Created를 반환한다") {
        // Given
        val request =
            Request(
                email = "test@example.com",
                verificationCode = "123456",
            )

        // When
        val response = controller.verify(request)

        // Then
        response.statusCode shouldBe HttpStatus.CREATED
        response.headers["Location"] shouldBe listOf("/api/v1/users/login")
        response.body shouldBe null

        fakeUseCase.verifyCalled shouldBe true
        fakeUseCase.lastEmail?.value shouldBe "test@example.com"
        fakeUseCase.lastCode?.value shouldBe "123456"
    }

    test("Location 헤더는 항상 /api/v1/users/login을 가리킨다") {
        // Given
        val requests =
            listOf(
                Request("user1@example.com", "111111"),
                Request("user2@test.com", "222222"),
                Request("admin@company.co.kr", "333333"),
            )

        requests.forEach { request ->
            // When
            val response = controller.verify(request)

            // Then
            response.headers["Location"] shouldBe listOf("/api/v1/users/login")
        }
    }

    test("잘못된 인증 코드 시 InvalidVerificationCodeException을 전파한다") {
        // Given
        val request =
            Request(
                email = "test@example.com",
                verificationCode = "999999",
            )
        fakeUseCase.shouldThrowException = InvalidVerificationCodeException()

        // When & Then
        val exception =
            shouldThrow<InvalidVerificationCodeException> {
                controller.verify(request)
            }
        exception.message shouldBe "Invalid verification code"
    }

    test("존재하지 않는 이메일 시 UserNotFoundException을 전파한다") {
        // Given
        val email = "nonexistent@example.com"
        val request =
            Request(
                email = email,
                verificationCode = "123456",
            )
        fakeUseCase.shouldThrowException = UserNotFoundException(email)

        // When & Then
        val exception =
            shouldThrow<UserNotFoundException> {
                controller.verify(request)
            }
        exception.message shouldBe "User not found: nonexistent@example.com"
    }

    test("다양한 입력값으로 UseCase가 올바르게 호출되는지 확인한다") {
        // Given
        val testCases =
            listOf(
                Request(
                    email = "user1@example.com",
                    verificationCode = "111111",
                ),
                Request(
                    email = "user2@test.com",
                    verificationCode = "222222",
                ),
                Request(
                    email = "admin@company.co.kr",
                    verificationCode = "999999",
                ),
            )

        testCases.forEach { request ->
            // Given
            fakeUseCase.reset()

            // When
            val response = controller.verify(request)

            // Then
            response.statusCode shouldBe HttpStatus.CREATED
            fakeUseCase.lastEmail?.value shouldBe request.email
            fakeUseCase.lastCode?.value shouldBe request.verificationCode
        }
    }
})
