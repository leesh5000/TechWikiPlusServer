package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakePasswordEncoder
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import me.helloc.techwikiplus.user.infrastructure.refreshtoken.fake.FakeRefreshTokenStore
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import me.helloc.techwikiplus.user.infrastructure.security.fake.FakeTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserLoginUseCaseUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userReader: UserReader
    private lateinit var userAuthenticator: UserAuthenticator
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var refreshTokenStore: FakeRefreshTokenStore
    private lateinit var jwtProperties: JwtProperties
    private lateinit var userLoginUseCase: UserLoginUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userReader = UserReader(userRepository)
        passwordEncoder = FakePasswordEncoder()
        userAuthenticator = UserAuthenticator(passwordEncoder)
        tokenProvider = FakeTokenProvider()
        refreshTokenStore = FakeRefreshTokenStore()
        jwtProperties =
            JwtProperties().apply {
                refreshTokenExpiration = 604800000 // 7 days
            }
        userLoginUseCase =
            UserLoginUseCase(
                userReader = userReader,
                userAuthenticator = userAuthenticator,
                tokenProvider = tokenProvider,
                refreshTokenStore = refreshTokenStore,
                tokenConfiguration = jwtProperties,
            )
    }

    @Test
    @DisplayName("유효한 자격 증명으로 로그인 성공")
    fun shouldLoginSuccessfullyWithValidCredentials() {
        // given
        val email = "test@example.com"
        val password = "password123"
        val userId = 1L
        val encodedPassword = passwordEncoder.encode(password)

        val user =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)

        // when
        val result = userLoginUseCase.login(email, password)

        // then
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()

        // 토큰에 올바른 정보가 포함되었는지 확인
        val accessTokenEmail = tokenProvider.getEmailFromToken(result.accessToken)
        val accessTokenUserId = tokenProvider.getUserIdFromToken(result.accessToken)
        assertThat(accessTokenEmail).isEqualTo(email)
        assertThat(accessTokenUserId).isEqualTo(userId)

        val refreshTokenEmail = tokenProvider.getEmailFromToken(result.refreshToken)
        val refreshTokenUserId = tokenProvider.getUserIdFromToken(result.refreshToken)
        assertThat(refreshTokenEmail).isEqualTo(email)
        assertThat(refreshTokenUserId).isEqualTo(userId)

        // Refresh token이 저장되었는지 확인
        assertThat(refreshTokenStore.exists(result.refreshToken)).isTrue
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 예외 발생")
    fun shouldThrowExceptionWhenUserNotFound() {
        // given
        val email = "nonexistent@example.com"
        val password = "password123"

        // when & then
        assertThatThrownBy {
            userLoginUseCase.login(email, password)
        }.isInstanceOf(CustomException.NotFoundException.UserEmailNotFoundException::class.java)
            .hasMessage("User not found with email: $email")
    }

    @Test
    @DisplayName("비밀번호가 틀렸을 때 예외 발생")
    fun shouldThrowExceptionWhenPasswordIsIncorrect() {
        // given
        val email = "test@example.com"
        val correctPassword = "password123"
        val wrongPassword = "wrongpassword"
        val userId = 1L
        val encodedPassword = passwordEncoder.encode(correctPassword)

        val user =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)

        // when & then
        assertThatThrownBy {
            userLoginUseCase.login(email, wrongPassword)
        }.isInstanceOf(CustomException.AuthenticationException.InvalidCredentials::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    @DisplayName("사용자가 대기 상태일 때 예외 발생")
    fun shouldThrowExceptionWhenUserIsPending() {
        // given
        val email = "pending@example.com"
        val password = "password123"
        val userId = 1L
        val encodedPassword = passwordEncoder.encode(password)

        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = "pendinguser",
                password = encodedPassword,
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // when & then
        assertThatThrownBy {
            userLoginUseCase.login(email, password)
        }.isInstanceOf(CustomException.AuthenticationException.EmailNotVerified::class.java)
            .hasMessage("Email not verified. Please verify your email before logging in.")
    }

    @Test
    @DisplayName("로그인할 때마다 다른 토큰 생성")
    fun shouldGenerateDifferentTokensForEachLogin() {
        // given
        val email = "test@example.com"
        val password = "password123"
        val userId = 1L
        val encodedPassword = passwordEncoder.encode(password)

        val user =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)

        // when
        val result1 = userLoginUseCase.login(email, password)
        val result2 = userLoginUseCase.login(email, password)

        // then
        assertThat(result1.accessToken).isNotEqualTo(result2.accessToken)
        assertThat(result1.refreshToken).isNotEqualTo(result2.refreshToken)
    }

    @Test
    @DisplayName("로그인 결과에 올바른 사용자 ID 반환")
    fun shouldReturnCorrectUserIdInLoginResult() {
        // given
        val users =
            listOf(
                Triple(1L, "user1@example.com", "password1"),
                Triple(2L, "user2@example.com", "password2"),
                Triple(3L, "user3@example.com", "password3"),
            )

        users.forEach { (id, email, password) ->
            val encodedPassword = passwordEncoder.encode(password)
            val user =
                User(
                    id = id,
                    email = UserEmail(email, true),
                    nickname = "user$id",
                    password = encodedPassword,
                    status = UserStatus.ACTIVE,
                    createdAt = Clock.system.localDateTime(),
                    updatedAt = Clock.system.localDateTime(),
                )
            userRepository.insertOrUpdate(user)
        }

        // when & then
        users.forEach { (expectedId, email, password) ->
            val result = userLoginUseCase.login(email, password)
            assertThat(result.userId).isEqualTo(expectedId)
        }
    }
}
