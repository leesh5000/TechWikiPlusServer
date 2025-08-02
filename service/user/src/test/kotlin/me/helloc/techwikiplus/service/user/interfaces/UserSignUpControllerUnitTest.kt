package me.helloc.techwikiplus.service.user.interfaces

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.interfaces.UserSignUpController.UserSignUpRequest
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserSignUpUseCase
import org.springframework.http.HttpStatus

class UserSignUpControllerUnitTest : FunSpec({

    // Fake implementation for testing
    class FakeUserSignUpUseCase : UserSignUpUseCase {
        var signupCalled = false
        var lastEmail: String? = null
        var lastPassword: String? = null
        var lastConfirmPassword: String? = null
        var lastNickname: String? = null
        var shouldThrowException: Exception? = null

        override fun signup(
            email: String,
            password: String,
            confirmPassword: String,
            nickname: String,
        ) {
            signupCalled = true
            lastEmail = email
            lastPassword = password
            lastConfirmPassword = confirmPassword
            lastNickname = nickname

            shouldThrowException?.let { throw it }
        }

        fun reset() {
            signupCalled = false
            lastEmail = null
            lastPassword = null
            lastConfirmPassword = null
            lastNickname = null
            shouldThrowException = null
        }
    }

    val fakeUseCase = FakeUserSignUpUseCase()
    val controller = UserSignUpController(fakeUseCase)

    beforeEach {
        fakeUseCase.reset()
    }

    test("회원가입 요청 시 UseCase를 호출하고 202 Accepted를 반환한다") {
        // Given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When
        val response = controller.signup(request)

        // Then
        response.statusCode shouldBe HttpStatus.ACCEPTED
        response.headers["Location"] shouldBe listOf("/api/v1/users/verify")
        response.body shouldBe null

        fakeUseCase.signupCalled shouldBe true
        fakeUseCase.lastEmail shouldBe "test@example.com"
        fakeUseCase.lastNickname shouldBe "테스터"
        fakeUseCase.lastPassword shouldBe "Test1234!"
        fakeUseCase.lastConfirmPassword shouldBe "Test1234!"
    }

    test("이메일 중복 시 UserAlreadyExistsException을 전파한다") {
        // Given
        val email: String = "existing@example.com"
        val request =
            UserSignUpRequest(
                email = email,
                nickname = "신규사용자",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )
        fakeUseCase.shouldThrowException = UserAlreadyExistsException(email)

        // When & Then
        val exception =
            shouldThrow<UserAlreadyExistsException> {
                controller.signup(request)
            }
        exception.message shouldBe "User with email existing@example.com already exists"
    }

    test("비밀번호 불일치 시 PasswordMismatchException을 전파한다") {
        // Given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Different1234!",
            )
        fakeUseCase.shouldThrowException = PasswordMismatchException("Test1234!")

        // When & Then
        val exception =
            shouldThrow<PasswordMismatchException> {
                controller.signup(request)
            }
        exception.message shouldBe "Password and confirmation do not match: Test1234!"
    }

    test("다양한 입력값으로 UseCase가 올바르게 호출되는지 확인한다") {
        // Given
        val testCases =
            listOf(
                UserSignUpRequest(
                    email = "user1@example.com",
                    nickname = "사용자1",
                    password = "Password1!",
                    confirmPassword = "Password1!",
                ),
                UserSignUpRequest(
                    email = "user2@test.com",
                    nickname = "사용자2",
                    password = "Password2@",
                    confirmPassword = "Password2@",
                ),
                UserSignUpRequest(
                    email = "admin@company.co.kr",
                    nickname = "관리자",
                    password = "AdminPass3#",
                    confirmPassword = "AdminPass3#",
                ),
            )

        testCases.forEach { request ->
            // Given
            fakeUseCase.reset()

            // When
            val response = controller.signup(request)

            // Then
            response.statusCode shouldBe HttpStatus.ACCEPTED
            fakeUseCase.lastEmail shouldBe request.email
            fakeUseCase.lastNickname shouldBe request.nickname
            fakeUseCase.lastPassword shouldBe request.password
            fakeUseCase.lastConfirmPassword shouldBe request.confirmPassword
        }
    }

    test("Location 헤더는 항상 /api/v1/users/verify를 가리킨다") {
        // Given
        val requests =
            listOf(
                UserSignUpRequest("a@b.com", "닉네임1", "Pass1234!", "Pass1234!"),
                UserSignUpRequest("x@y.com", "닉네임2", "Pass5678@", "Pass5678@"),
            )

        requests.forEach { request ->
            // When
            val response = controller.signup(request)

            // Then
            response.headers["Location"] shouldBe listOf("/api/v1/users/verify")
        }
    }
})
