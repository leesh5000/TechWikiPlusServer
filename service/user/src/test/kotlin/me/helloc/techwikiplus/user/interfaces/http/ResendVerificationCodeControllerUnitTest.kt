package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.ResendVerificationCodeUseCase
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus

class ResendVerificationCodeControllerUnitTest {
    private lateinit var resendVerificationCodeUseCase: ResendVerificationCodeUseCase
    private lateinit var controller: ResendVerificationCodeController

    @BeforeEach
    fun setUp() {
        resendVerificationCodeUseCase = mock(ResendVerificationCodeUseCase::class.java)
        controller = ResendVerificationCodeController(resendVerificationCodeUseCase)
    }

    @Test
    fun shouldResendVerificationCodeSuccessfullyAndReturnAcceptedStatus() {
        // given
        val email = "test@example.com"

        // when
        val response = controller.resendVerificationCode(email)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        assertThat(response.body).isNull()

        verify(resendVerificationCodeUseCase).resendVerificationCode(email)
    }

    @Test
    fun shouldCallUseCaseWithCorrectEmail() {
        // given
        val email = "user@domain.com"

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCase, times(1)).resendVerificationCode("user@domain.com")
    }

    @Test
    fun shouldHandleEmailWithSpecialCharacters() {
        // given
        val email = "test+user@example.com"

        // when
        val response = controller.resendVerificationCode(email)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        verify(resendVerificationCodeUseCase).resendVerificationCode(email)
    }

    @Test
    fun shouldHandleEmailWithSubdomain() {
        // given
        val email = "user@mail.example.com"

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCase).resendVerificationCode(email)
    }

    @Test
    fun shouldPropagateExceptionWhenUserNotFound() {
        // given
        val email = "nonexistent@example.com"

        val exception = CustomException.AuthenticationException.PendingUserNotFound(email)
        doThrow(exception).`when`(resendVerificationCodeUseCase).resendVerificationCode(email)

        // when & then
        try {
            controller.resendVerificationCode(email)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.AuthenticationException.PendingUserNotFound) {
            assertThat(e.email).isEqualTo(email)
        }
    }

    @Test
    fun shouldPropagateExceptionWhenRateLimitExceeded() {
        // given
        val email = "test@example.com"

        val exception = CustomException.ResendRateLimitExceeded("Too many requests. Please try again later.")
        doThrow(exception).`when`(resendVerificationCodeUseCase).resendVerificationCode(email)

        // when & then
        try {
            controller.resendVerificationCode(email)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.ResendRateLimitExceeded) {
            assertThat(e.message).contains("Too many requests")
        }
    }

    @Test
    fun shouldHandleEmptyEmail() {
        // given
        val email = ""

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCase).resendVerificationCode("")
    }

    @Test
    fun shouldReturnEmptyResponseBodyOnSuccess() {
        // given
        val email = "test@example.com"

        // when
        val response = controller.resendVerificationCode(email)

        // then
        assertThat(response.body).isNull()
        assertThat(response.hasBody()).isFalse()
    }

    @Test
    fun shouldHandleMultipleResendRequests() {
        // given
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"

        // when
        controller.resendVerificationCode(email1)
        controller.resendVerificationCode(email2)
        controller.resendVerificationCode(email1)

        // then
        verify(resendVerificationCodeUseCase, times(2)).resendVerificationCode(email1)
        verify(resendVerificationCodeUseCase, times(1)).resendVerificationCode(email2)
    }
}
