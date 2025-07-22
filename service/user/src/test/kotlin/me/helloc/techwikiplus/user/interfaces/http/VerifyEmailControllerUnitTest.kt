package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.VerifyEmailUseCase
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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

        verify(verifyEmailUseCase).verifyEmail(
            email = request.email,
            code = request.code,
        )
    }

    @Test
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
        verify(verifyEmailUseCase, times(1)).verifyEmail(
            email = "user@domain.com",
            code = "ABCD1234",
        )
    }

    @Test
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
        verify(verifyEmailUseCase).verifyEmail(
            email = request.email,
            code = request.code,
        )
    }

    @Test
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
        verify(verifyEmailUseCase).verifyEmail(
            email = request.email,
            code = request.code,
        )
    }

    @Test
    fun shouldPropagateExceptionWhenVerificationCodeExpired() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "123456",
            )

        val exception = CustomException.AuthenticationException.ExpiredEmailVerification("test@example.com")
        doThrow(exception).`when`(verifyEmailUseCase).verifyEmail(
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
    fun shouldPropagateExceptionWhenInvalidVerificationCode() {
        // given
        val request =
            VerifyEmailController.UserSignUpVerifyRequest(
                email = "test@example.com",
                code = "WRONG",
            )

        val exception = CustomException.AuthenticationException.InvalidVerificationCode("WRONG")
        doThrow(exception).`when`(verifyEmailUseCase).verifyEmail(
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
        verify(verifyEmailUseCase).verifyEmail(
            email = "",
            code = "",
        )
    }

    @Test
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
