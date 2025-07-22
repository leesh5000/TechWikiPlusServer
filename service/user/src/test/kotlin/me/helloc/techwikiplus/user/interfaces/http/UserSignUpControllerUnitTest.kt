package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserSignUpUseCase
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus

class UserSignUpControllerUnitTest {
    private lateinit var userSignUpUseCase: UserSignUpUseCase
    private lateinit var controller: UserSignUpController

    @BeforeEach
    fun setUp() {
        userSignUpUseCase = mock(UserSignUpUseCase::class.java)
        controller = UserSignUpController(userSignUpUseCase)
    }

    @Test
    fun shouldSignUpSuccessfullyAndReturnAcceptedStatus() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "test@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when
        val response = controller.signUp(request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        assertThat(response.headers["Location"]).containsExactly("/api/v1/users/signup/verify")
        assertThat(response.body).isNull()

        verify(userSignUpUseCase).signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
    }

    @Test
    fun shouldCallUseCaseWithCorrectParameters() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "user@domain.com",
                nickname = "newuser",
                password = "Password123!",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCase, times(1)).signUp(
            email = "user@domain.com",
            nickname = "newuser",
            password = "Password123!",
        )
    }

    @Test
    fun shouldReturnLocationHeaderWithVerifyEndpoint() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "test@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when
        val response = controller.signUp(request)

        // then
        assertThat(response.headers.containsKey("Location")).isTrue()
        assertThat(response.headers["Location"]).containsExactly("/api/v1/users/signup/verify")
    }

    @Test
    fun shouldPropagateExceptionWhenUseCaseThrowsException() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "duplicate@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        val exception = CustomException.ConflictException.DuplicateEmail("duplicate@example.com")
        doThrow(exception).`when`(userSignUpUseCase).signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )

        // when & then
        try {
            controller.signUp(request)
            assertThat(false).isTrue() // should not reach here
        } catch (e: CustomException.ConflictException.DuplicateEmail) {
            assertThat(e.email).isEqualTo("duplicate@example.com")
        }
    }

    @Test
    fun shouldHandleEmptyRequestFields() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "",
                nickname = "",
                password = "",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCase).signUp(
            email = "",
            nickname = "",
            password = "",
        )
    }

    @Test
    fun shouldHandleSpecialCharactersInNickname() {
        // given
        val request =
            UserSignUpController.UserSignUpRequest(
                email = "test@example.com",
                nickname = "test_user-123",
                password = "ValidPass123!",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCase).signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
    }
}
