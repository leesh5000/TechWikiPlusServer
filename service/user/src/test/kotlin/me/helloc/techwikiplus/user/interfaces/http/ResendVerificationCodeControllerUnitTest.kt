package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.infrastructure.usecase.ResendVerificationCodeUseCaseWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class ResendVerificationCodeControllerUnitTest {
    private lateinit var resendVerificationCodeUseCaseWrapper: ResendVerificationCodeUseCaseWrapper
    private lateinit var controller: ResendVerificationCodeController

    @BeforeEach
    fun setUp() {
        resendVerificationCodeUseCaseWrapper = mock(ResendVerificationCodeUseCaseWrapper::class.java)
        controller = ResendVerificationCodeController(resendVerificationCodeUseCaseWrapper)
    }

    @Test
    @DisplayName("인증 코드 재전송 성공 시 202 Accepted 상태 코드 반환")
    fun shouldResendVerificationCodeSuccessfullyAndReturnAcceptedStatus() {
        // given
        val email = "test@example.com"

        // when
        val response = controller.resendVerificationCode(email)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        assertThat(response.body).isNull()

        verify(resendVerificationCodeUseCaseWrapper).resendVerificationCode(email)
    }

    @Test
    @DisplayName("UseCase에 올바른 이메일 전달")
    fun shouldCallUseCaseWithCorrectEmail() {
        // given
        val email = "user@domain.com"

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCaseWrapper, times(1)).resendVerificationCode("user@domain.com")
    }

    @Test
    @DisplayName("특수문자가 포함된 이메일 처리")
    fun shouldHandleEmailWithSpecialCharacters() {
        // given
        val email = "test+user@example.com"

        // when
        val response = controller.resendVerificationCode(email)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        verify(resendVerificationCodeUseCaseWrapper).resendVerificationCode(email)
    }

    @Test
    @DisplayName("서브도메인이 포함된 이메일 처리")
    fun shouldHandleEmailWithSubdomain() {
        // given
        val email = "user@mail.example.com"

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCaseWrapper).resendVerificationCode(email)
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 예외 전파")
    fun shouldPropagateExceptionWhenUserNotFound() {
        // given
        val email = "nonexistent@example.com"

        val exception = CustomException.AuthenticationException.PendingUserNotFound(email)
        doThrow(exception).`when`(resendVerificationCodeUseCaseWrapper).resendVerificationCode(email)

        // when & then
        try {
            controller.resendVerificationCode(email)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.AuthenticationException.PendingUserNotFound) {
            assertThat(e.email).isEqualTo(email)
        }
    }

    @Test
    @DisplayName("전송 제한을 초과했을 때 예외 전파")
    fun shouldPropagateExceptionWhenRateLimitExceeded() {
        // given
        val email = "test@example.com"

        val exception = CustomException.ResendRateLimitExceeded("Too many requests. Please try again later.")
        doThrow(exception).`when`(resendVerificationCodeUseCaseWrapper).resendVerificationCode(email)

        // when & then
        try {
            controller.resendVerificationCode(email)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.ResendRateLimitExceeded) {
            assertThat(e.message).contains("Too many requests")
        }
    }

    @Test
    @DisplayName("비어있는 이메일 처리")
    fun shouldHandleEmptyEmail() {
        // given
        val email = ""

        // when
        controller.resendVerificationCode(email)

        // then
        verify(resendVerificationCodeUseCaseWrapper).resendVerificationCode("")
    }

    @Test
    @DisplayName("성공 시 비어있는 응답 본문 반환")
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
    @DisplayName("다중 재전송 요청 처리")
    fun shouldHandleMultipleResendRequests() {
        // given
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"

        // when
        controller.resendVerificationCode(email1)
        controller.resendVerificationCode(email2)
        controller.resendVerificationCode(email1)

        // then
        verify(resendVerificationCodeUseCaseWrapper, times(2)).resendVerificationCode(email1)
        verify(resendVerificationCodeUseCaseWrapper, times(1)).resendVerificationCode(email2)
    }
}
