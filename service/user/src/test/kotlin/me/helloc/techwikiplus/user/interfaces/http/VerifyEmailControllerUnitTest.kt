package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.VerifyEmailUseCase
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus

class VerifyEmailControllerUnitTest {
    private lateinit var verifyEmailUseCase: VerifyEmailUseCase
    private lateinit var controller: VerifyEmailController

    @BeforeEach
    fun setUp() {
        verifyEmailUseCase = mock(VerifyEmailUseCase::class.java)
        controller = VerifyEmailController(verifyEmailUseCase)
    }

    @Test
    @DisplayName("이메일 인증 성공 시 200 OK 상태 코드 반환")
    fun shouldVerifyEmailSuccessfullyAndReturnOkStatus() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "123456",
            )

        // when
        val response = controller.verifyEmail(request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()

        verify(verifyEmailUseCase).verify(
            email = request.email,
            code = request.code,
        )
    }

    @Test
    @DisplayName("UseCase에 올바른 파라미터 전달")
    fun shouldCallUseCaseWithCorrectParameters() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "user@domain.com",
                code = "ABCD1234",
            )

        // when
        controller.verifyEmail(request)

        // then
        verify(verifyEmailUseCase, times(1)).verify(
            email = "user@domain.com",
            code = "ABCD1234",
        )
    }

    @Test
    @DisplayName("숫자로만 구성된 인증 코드 처리")
    fun shouldHandleNumericVerificationCode() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "999999",
            )

        // when
        val response = controller.verifyEmail(request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(verifyEmailUseCase).verify(
            email = request.email,
            code = request.code,
        )
    }

    @Test
    @DisplayName("영숫자 혼합 인증 코드 처리")
    fun shouldHandleAlphanumericVerificationCode() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "ABC123XYZ",
            )

        // when
        controller.verifyEmail(request)

        // then
        verify(verifyEmailUseCase).verify(
            email = request.email,
            code = request.code,
        )
    }

    @Test
    @DisplayName("인증 코드가 만료되었을 때 예외 전파")
    fun shouldPropagateExceptionWhenVerificationCodeExpired() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "123456",
            )

        val exception = CustomException.AuthenticationException.ExpiredEmailVerification("test@example.com")
        doThrow(exception).`when`(verifyEmailUseCase).verify(
            email = request.email,
            code = request.code,
        )

        // when & then
        try {
            controller.verifyEmail(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.AuthenticationException.ExpiredEmailVerification) {
            assertThat(e.email).isEqualTo("test@example.com")
        }
    }

    @Test
    @DisplayName("유효하지 않은 인증 코드일 때 예외 전파")
    fun shouldPropagateExceptionWhenInvalidVerificationCode() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "WRONG",
            )

        val exception = CustomException.AuthenticationException.InvalidVerificationCode("WRONG")
        doThrow(exception).`when`(verifyEmailUseCase).verify(
            email = request.email,
            code = request.code,
        )

        // when & then
        try {
            controller.verifyEmail(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.AuthenticationException.InvalidVerificationCode) {
            assertThat(e.code).isEqualTo("WRONG")
        }
    }

    @Test
    @DisplayName("비어있는 필드 처리")
    fun shouldHandleEmptyFields() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "",
                code = "",
            )

        // when
        controller.verifyEmail(request)

        // then
        verify(verifyEmailUseCase).verify(
            email = "",
            code = "",
        )
    }

    @Test
    @DisplayName("성공 시 비어있는 응답 본문 반환")
    fun shouldReturnEmptyResponseBodyOnSuccess() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "123456",
            )

        // when
        val response = controller.verifyEmail(request)

        // then
        assertThat(response.body).isNull()
        assertThat(response.hasBody()).isFalse()
    }
}
