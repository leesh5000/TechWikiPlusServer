package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.TokenResult
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import me.helloc.techwikiplus.user.domain.exception.notfound.UserEmailNotFoundException
import me.helloc.techwikiplus.user.infrastructure.usecase.UserLoginUseCaseWrapper
import me.helloc.techwikiplus.user.interfaces.http.dto.request.LoginRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class UserLoginControllerUnitTest {
    private lateinit var userLoginUseCaseWrapper: UserLoginUseCaseWrapper
    private lateinit var controller: UserLoginController

    @BeforeEach
    fun setUp() {
        userLoginUseCaseWrapper = mock(UserLoginUseCaseWrapper::class.java)
        controller = UserLoginController(userLoginUseCaseWrapper)
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 반환")
    fun shouldLoginSuccessfullyAndReturnTokens() {
        // given
        val request =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val useCaseResult =
            TokenResult(
                accessToken = "access-token-123",
                refreshToken = "refresh-token-456",
                userId = 1L,
            )

        `when`(userLoginUseCaseWrapper.login(request.email, request.password))
            .thenReturn(useCaseResult)

        // when
        val response = controller.login(request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.accessToken).isEqualTo("access-token-123")
        assertThat(response.body!!.refreshToken).isEqualTo("refresh-token-456")
        assertThat(response.body!!.userId).isEqualTo(1L)

        verify(userLoginUseCaseWrapper).login(
            email = request.email,
            password = request.password,
        )
    }

    @Test
    @DisplayName("UseCase에 올바른 파라미터 전달")
    fun shouldCallUseCaseWithCorrectParameters() {
        // given
        val request =
            LoginRequest(
                email = "user@domain.com",
                password = "securePassword",
            )

        val useCaseResult =
            TokenResult(
                accessToken = "token",
                refreshToken = "refresh",
                userId = 123L,
            )

        `when`(userLoginUseCaseWrapper.login(anyString(), anyString()))
            .thenReturn(useCaseResult)

        // when
        controller.login(request)

        // then
        verify(userLoginUseCaseWrapper, times(1)).login(
            email = "user@domain.com",
            password = "securePassword",
        )
    }

    @Test
    @DisplayName("서로 다른 사용자에게 다른 토큰 반환")
    fun shouldReturnDifferentTokensForDifferentUsers() {
        // given
        val request1 =
            LoginRequest(
                email = "user1@example.com",
                password = "password1",
            )

        val request2 =
            LoginRequest(
                email = "user2@example.com",
                password = "password2",
            )

        val result1 =
            TokenResult(
                accessToken = "access1",
                refreshToken = "refresh1",
                userId = 1L,
            )

        val result2 =
            TokenResult(
                accessToken = "access2",
                refreshToken = "refresh2",
                userId = 2L,
            )

        `when`(userLoginUseCaseWrapper.login(request1.email, request1.password))
            .thenReturn(result1)
        `when`(userLoginUseCaseWrapper.login(request2.email, request2.password))
            .thenReturn(result2)

        // when
        val response1 = controller.login(request1)
        val response2 = controller.login(request2)

        // then
        assertThat(response1.body!!.accessToken).isNotEqualTo(response2.body!!.accessToken)
        assertThat(response1.body!!.refreshToken).isNotEqualTo(response2.body!!.refreshToken)
        assertThat(response1.body!!.userId).isNotEqualTo(response2.body!!.userId)
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 예외 전파")
    fun shouldPropagateExceptionWhenUserNotFound() {
        // given
        val request =
            LoginRequest(
                email = "nonexistent@example.com",
                password = "password123",
            )

        val exception = UserEmailNotFoundException("nonexistent@example.com")
        doThrow(exception).`when`(userLoginUseCaseWrapper).login(
            email = request.email,
            password = request.password,
        )

        // when & then
        try {
            controller.login(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: UserEmailNotFoundException) {
            assertThat(e.email).isEqualTo("nonexistent@example.com")
        }
    }

    @Test
    @DisplayName("잘못된 자격 증명일 때 예외 전파")
    fun shouldPropagateExceptionWhenInvalidCredentials() {
        // given
        val request =
            LoginRequest(
                email = "test@example.com",
                password = "wrongPassword",
            )

        val exception = InvalidCredentialsException()
        doThrow(exception).`when`(userLoginUseCaseWrapper).login(
            email = request.email,
            password = request.password,
        )

        // when & then
        try {
            controller.login(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: InvalidCredentialsException) {
            assertThat(e.message).isEqualTo("Invalid email or password")
        }
    }

    @Test
    @DisplayName("비어있는 이메일과 비밀번호 처리")
    fun shouldHandleEmptyEmailAndPassword() {
        // given
        val request =
            LoginRequest(
                email = "",
                password = "",
            )

        val result =
            TokenResult(
                accessToken = "token",
                refreshToken = "refresh",
                userId = 1L,
            )

        `when`(userLoginUseCaseWrapper.login("", ""))
            .thenReturn(result)

        // when
        controller.login(request)

        // then
        verify(userLoginUseCaseWrapper).login("", "")
    }

    @Test
    @DisplayName("응답 구조 유지")
    fun shouldMaintainResponseStructure() {
        // given
        val request =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val useCaseResult =
            TokenResult(
                accessToken = "very-long-access-token-with-many-characters",
                refreshToken = "very-long-refresh-token-with-many-characters",
                userId = 999999L,
            )

        `when`(userLoginUseCaseWrapper.login(request.email, request.password))
            .thenReturn(useCaseResult)

        // when
        val response = controller.login(request)

        // then
        assertThat(response.body).isNotNull
        assertThat(response.body!!.accessToken).isEqualTo(useCaseResult.accessToken)
        assertThat(response.body!!.refreshToken).isEqualTo(useCaseResult.refreshToken)
        assertThat(response.body!!.userId).isEqualTo(useCaseResult.userId)
    }
}
