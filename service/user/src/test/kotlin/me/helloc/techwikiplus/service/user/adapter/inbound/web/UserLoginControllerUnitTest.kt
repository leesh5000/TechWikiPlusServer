package me.helloc.techwikiplus.service.user.adapter.inbound.web

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.application.port.inbound.UserLoginUseCase
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import java.time.Instant

class FakeUserLoginUseCase : UserLoginUseCase {
    var stubResult: UserLoginUseCase.Result? = null
    var stubException: Exception? = null
    var lastCommand: UserLoginUseCase.Command? = null

    override fun execute(command: UserLoginUseCase.Command): UserLoginUseCase.Result {
        lastCommand = command
        stubException?.let { throw it }
        return stubResult ?: throw IllegalStateException("No stub result provided")
    }
}

class UserLoginControllerUnitTest : FunSpec({
    lateinit var controller: UserLoginController
    lateinit var fakeUseCase: FakeUserLoginUseCase

    beforeEach {
        fakeUseCase = FakeUserLoginUseCase()
        controller = UserLoginController(fakeUseCase)
    }

    test("로그인 요청 시 UseCase를 호출하고 200 OK와 토큰 정보를 반환한다") {
        // Given
        val request =
            UserLoginController.Request(
                email = "user@example.com",
                password = "password123",
            )
        val expectedResult =
            UserLoginUseCase.Result(
                accessToken = "access-token-value",
                refreshToken = "refresh-token-value",
                userId = "user-123",
                accessTokenExpiresAt = Instant.now().plusSeconds(7200),
                refreshTokenExpiresAt = Instant.now().plusSeconds(604800),
            )
        fakeUseCase.stubResult = expectedResult

        // When
        val response = controller.login(request)

        // Then
        response.statusCode shouldBe HttpStatus.OK
        response.body?.accessToken shouldBe expectedResult.accessToken
        response.body?.refreshToken shouldBe expectedResult.refreshToken
        response.body?.userId shouldBe expectedResult.userId
        response.body?.accessTokenExpiresAt shouldBe expectedResult.accessTokenExpiresAt
        response.body?.refreshTokenExpiresAt shouldBe expectedResult.refreshTokenExpiresAt

        fakeUseCase.lastCommand?.email shouldBe request.email
        fakeUseCase.lastCommand?.password shouldBe request.password
    }

    test("잘못된 비밀번호로 로그인 시 InvalidCredentialsException을 전파한다") {
        // Given
        val request =
            UserLoginController.Request(
                email = "user@example.com",
                password = "wrong-password",
            )
        fakeUseCase.stubException = InvalidCredentialsException()

        // When/Then
        shouldThrow<InvalidCredentialsException> {
            controller.login(request)
        }
    }

    test("존재하지 않는 사용자로 로그인 시 UserNotFoundException을 전파한다") {
        // Given
        val request =
            UserLoginController.Request(
                email = "nonexistent@example.com",
                password = "password123",
            )
        fakeUseCase.stubException = UserNotFoundException("nonexistent@example.com")

        // When/Then
        shouldThrow<UserNotFoundException> {
            controller.login(request)
        }
    }

    test("다양한 입력값으로 UseCase가 올바르게 호출되는지 확인한다") {
        val testCases =
            listOf(
                UserLoginController.Request("test1@example.com", "pass1"),
                UserLoginController.Request("test2@example.com", "pass2"),
                UserLoginController.Request("special+char@example.com", "p@ssw0rd!"),
                UserLoginController.Request("korean@이메일.com", "한글비밀번호123"),
            )

        testCases.forEach { request ->
            // Given
            fakeUseCase.stubResult =
                UserLoginUseCase.Result(
                    accessToken = "token",
                    refreshToken = "refresh",
                    userId = "user-123",
                    accessTokenExpiresAt = Instant.now().plusSeconds(7200),
                    refreshTokenExpiresAt = Instant.now().plusSeconds(604800),
                )

            // When
            controller.login(request)

            // Then
            fakeUseCase.lastCommand?.email shouldBe request.email
            fakeUseCase.lastCommand?.password shouldBe request.password
        }
    }
})
