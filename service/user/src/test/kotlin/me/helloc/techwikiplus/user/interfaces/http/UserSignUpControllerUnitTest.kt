package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.infrastructure.usecase.UserSignUpUseCaseWrapper
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpRequest
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

class UserSignUpControllerUnitTest {
    private lateinit var userSignUpUseCaseWrapper: UserSignUpUseCaseWrapper
    private lateinit var controller: UserSignUpController

    @BeforeEach
    fun setUp() {
        userSignUpUseCaseWrapper = mock(UserSignUpUseCaseWrapper::class.java)
        controller = UserSignUpController(userSignUpUseCaseWrapper)
    }

    @Test
    @DisplayName("회원가입 성공 시 202 Accepted 상태 코드 반환")
    fun shouldSignUpSuccessfullyAndReturnAcceptedStatus() {
        // given
        val request =
            UserSignUpRequest(
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

        verify(userSignUpUseCaseWrapper).signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
    }

    @Test
    @DisplayName("UseCase에 올바른 파라미터 전달")
    fun shouldCallUseCaseWithCorrectParameters() {
        // given
        val request =
            UserSignUpRequest(
                email = "user@domain.com",
                nickname = "newuser",
                password = "Password123!",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCaseWrapper, times(1)).signUp(
            email = "user@domain.com",
            nickname = "newuser",
            password = "Password123!",
        )
    }

    @Test
    @DisplayName("Location 헤더에 인증 엔드포인트 포함")
    fun shouldReturnLocationHeaderWithVerifyEndpoint() {
        // given
        val request =
            UserSignUpRequest(
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
    @DisplayName("UseCase에서 예외 발생 시 예외 전파")
    fun shouldPropagateExceptionWhenUseCaseThrowsException() {
        // given
        val request =
            UserSignUpRequest(
                email = "duplicate@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        val exception = CustomException.ConflictException.DuplicateEmail("duplicate@example.com")
        doThrow(exception).`when`(userSignUpUseCaseWrapper).signUp(
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
    @DisplayName("비어있는 요청 필드 처리")
    fun shouldHandleEmptyRequestFields() {
        // given
        val request =
            UserSignUpRequest(
                email = "",
                nickname = "",
                password = "",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCaseWrapper).signUp(
            email = "",
            nickname = "",
            password = "",
        )
    }

    @Test
    @DisplayName("닉네임에 특수문자 포함 시 처리")
    fun shouldHandleSpecialCharactersInNickname() {
        // given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "test_user-123",
                password = "ValidPass123!",
            )

        // when
        controller.signUp(request)

        // then
        verify(userSignUpUseCaseWrapper).signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
    }
}
