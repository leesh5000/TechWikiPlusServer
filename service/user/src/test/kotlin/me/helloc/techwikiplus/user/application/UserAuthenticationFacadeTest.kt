package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

class UserAuthenticationFacadeTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userPasswordService: UserPasswordService

    @Mock
    private lateinit var clock: Clock

    private lateinit var userAuthenticationFacade: UserAuthenticationFacade

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        userAuthenticationFacade = UserAuthenticationFacade(userRepository, userPasswordService)
    }

    @Test
    fun `should authenticate verified user successfully`() {
        val email = "test@example.com"
        val password = "Password123!"
        val encodedPassword = "encodedPassword"
        val userId = 12345L
        val now = LocalDateTime.now()

        `when`(clock.now()).thenReturn(now)

        val verifiedEmail = UserEmail(email).verify()
        val user = User(
            id = userId,
            nickname = "testuser",
            email = verifiedEmail,
            password = encodedPassword,
            createdAt = now,
            modifiedAt = now
        )

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(userPasswordService.matches(password, encodedPassword)).thenReturn(true)

        val result = userAuthenticationFacade.authenticate(email, password)

        assertThat(result).isEqualTo(userId)
    }

    @Test
    fun `should throw exception when user not found`() {
        val email = "test@example.com"
        val password = "Password123!"

        `when`(userRepository.findByEmail(email)).thenReturn(null)

        assertThatThrownBy {
            userAuthenticationFacade.authenticate(email, password)
        }.isInstanceOf(CustomException.AuthenticationException.InvalidCredentials::class.java)
    }

    @Test
    fun `should throw exception when password does not match`() {
        val email = "test@example.com"
        val password = "Password123!"
        val encodedPassword = "encodedPassword"
        val now = LocalDateTime.now()

        `when`(clock.now()).thenReturn(now)

        val user = User(
            id = 12345L,
            nickname = "testuser",
            email = email,
            password = encodedPassword,
            createdAt = now,
            modifiedAt = now
        )

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(userPasswordService.matches(password, encodedPassword)).thenReturn(false)

        assertThatThrownBy {
            userAuthenticationFacade.authenticate(email, password)
        }.isInstanceOf(CustomException.AuthenticationException.InvalidCredentials::class.java)
    }

    @Test
    fun `should throw exception when user email is not verified`() {
        val email = "test@example.com"
        val password = "Password123!"
        val encodedPassword = "encodedPassword"
        val now = LocalDateTime.now()

        `when`(clock.now()).thenReturn(now)

        val unverifiedEmail = UserEmail(email)
        val user = User(
            id = 12345L,
            nickname = "testuser",
            email = unverifiedEmail,
            password = encodedPassword,
            createdAt = now,
            modifiedAt = now
        )

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(userPasswordService.matches(password, encodedPassword)).thenReturn(true)

        assertThatThrownBy {
            userAuthenticationFacade.authenticate(email, password)
        }.isInstanceOf(CustomException.AuthenticationException.UnauthorizedAccess::class.java)
            .hasMessageContaining("Email verification required")
    }
}
