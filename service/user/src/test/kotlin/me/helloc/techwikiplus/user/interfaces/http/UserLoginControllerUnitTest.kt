package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserLoginUseCase
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class UserLoginControllerUnitTest {
    private lateinit var userLoginUseCase: UserLoginUseCase
    private lateinit var controller: UserLoginController

    @BeforeEach
    fun setUp() {
        userLoginUseCase = mock(UserLoginUseCase::class.java)
        controller = UserLoginController(userLoginUseCase)
    }

    @Test
    fun shouldLoginSuccessfullyAndReturnTokens() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val useCaseResult =
            UserLoginUseCase.LoginResult(
                accessToken = "access-token-123",
                refreshToken = "refresh-token-456",
                userId = 1L,
            )

        `when`(userLoginUseCase.login(request.email, request.password))
            .thenReturn(useCaseResult)

        // when
        val response = controller.login(request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.accessToken).isEqualTo("access-token-123")
        assertThat(response.body!!.refreshToken).isEqualTo("refresh-token-456")
        assertThat(response.body!!.userId).isEqualTo(1L)

        verify(userLoginUseCase).login(
            email = request.email,
            password = request.password,
        )
    }

    @Test
    fun shouldCallUseCaseWithCorrectParameters() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "user@domain.com",
                password = "securePassword",
            )

        val useCaseResult =
            UserLoginUseCase.LoginResult(
                accessToken = "token",
                refreshToken = "refresh",
                userId = 123L,
            )

        `when`(userLoginUseCase.login(anyString(), anyString()))
            .thenReturn(useCaseResult)

        // when
        controller.login(request)

        // then
        verify(userLoginUseCase, times(1)).login(
            email = "user@domain.com",
            password = "securePassword",
        )
    }

    @Test
    fun shouldReturnDifferentTokensForDifferentUsers() {
        // given
        val request1 =
            UserLoginController.LoginRequest(
                email = "user1@example.com",
                password = "password1",
            )

        val request2 =
            UserLoginController.LoginRequest(
                email = "user2@example.com",
                password = "password2",
            )

        val result1 =
            UserLoginUseCase.LoginResult(
                accessToken = "access1",
                refreshToken = "refresh1",
                userId = 1L,
            )

        val result2 =
            UserLoginUseCase.LoginResult(
                accessToken = "access2",
                refreshToken = "refresh2",
                userId = 2L,
            )

        `when`(userLoginUseCase.login(request1.email, request1.password))
            .thenReturn(result1)
        `when`(userLoginUseCase.login(request2.email, request2.password))
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
    fun shouldPropagateExceptionWhenUserNotFound() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "nonexistent@example.com",
                password = "password123",
            )

        val exception = CustomException.NotFoundException.UserEmailNotFoundException("nonexistent@example.com")
        doThrow(exception).`when`(userLoginUseCase).login(
            email = request.email,
            password = request.password,
        )

        // when & then
        try {
            controller.login(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.NotFoundException.UserEmailNotFoundException) {
            assertThat(e.email).isEqualTo("nonexistent@example.com")
        }
    }

    @Test
    fun shouldPropagateExceptionWhenInvalidCredentials() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "test@example.com",
                password = "wrongPassword",
            )

        val exception = CustomException.AuthenticationException.InvalidCredentials()
        doThrow(exception).`when`(userLoginUseCase).login(
            email = request.email,
            password = request.password,
        )

        // when & then
        try {
            controller.login(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.AuthenticationException.InvalidCredentials) {
            assertThat(e.message).isEqualTo("Invalid email or password")
        }
    }

    @Test
    fun shouldHandleEmptyEmailAndPassword() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "",
                password = "",
            )

        val result =
            UserLoginUseCase.LoginResult(
                accessToken = "token",
                refreshToken = "refresh",
                userId = 1L,
            )

        `when`(userLoginUseCase.login("", ""))
            .thenReturn(result)

        // when
        controller.login(request)

        // then
        verify(userLoginUseCase).login("", "")
    }

    @Test
    fun shouldMaintainResponseStructure() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val useCaseResult =
            UserLoginUseCase.LoginResult(
                accessToken = "very-long-access-token-with-many-characters",
                refreshToken = "very-long-refresh-token-with-many-characters",
                userId = 999999L,
            )

        `when`(userLoginUseCase.login(request.email, request.password))
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
