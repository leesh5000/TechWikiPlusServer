package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.Duration

class UserSignUpFacadeTest {

    @Mock
    private lateinit var userWriter: UserWriter

    @Mock
    private lateinit var userDuplicateChecker: UserDuplicateChecker

    @Mock
    private lateinit var userPasswordService: UserPasswordService

    @Mock
    private lateinit var mailSender: MailSender

    @Mock
    private lateinit var verificationCodeStore: VerificationCodeStore

    @Mock
    private lateinit var pendingSignUpStore: PendingSignUpStore

    @Mock
    private lateinit var idGenerator: IdGenerator

    @Mock
    private lateinit var clock: Clock

    private lateinit var userSignUpUseCase: UserSignUpUseCase

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        userSignUpUseCase = UserSignUpUseCase(
            userWriter,
            userDuplicateChecker,
            userPasswordService,
            mailSender,
            verificationCodeStore,
            pendingSignUpStore,
            idGenerator,
            clock
        )
    }

    @Test
    fun `should initiate sign up process successfully`() {
        val email = "test@example.com"
        val nickname = "testuser"
        val password = "Password123!"
        val encodedPassword = "encodedPassword"
        val verificationCode = VerificationCode("123456")

        `when`(userPasswordService.validateAndEncode(password)).thenReturn(encodedPassword)
        `when`(mailSender.sendVerificationEmail(email)).thenReturn(verificationCode)

        userSignUpUseCase.signUp(email, nickname, password)

        verify(userDuplicateChecker).validateUserEmailDuplicate(email)
        verify(userDuplicateChecker).validateUserNicknameDuplicate(nickname)
        verify(pendingSignUpStore).store(email, nickname, encodedPassword, Duration.ofMinutes(5))
        verify(verificationCodeStore).storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))
    }

    @Test
    fun `should complete sign up after email verification`() {
        val email = "test@example.com"
        val code = "123456"
        val nickname = "testuser"
        val encodedPassword = "encodedPassword"
        val userId = 12345L
        val pendingData = PendingSignUpData(nickname, encodedPassword)

        `when`(pendingSignUpStore.retrieve(email)).thenReturn(pendingData)
        `when`(idGenerator.next()).thenReturn(userId)

        userSignUpUseCase.verifyEmail(email, code)

        verify(verificationCodeStore).retrieveOrThrows(email, VerificationCode(code))
        verify(userWriter).insertOrUpdate(any(User::class.java))
        verify(pendingSignUpStore).remove(email)
    }

    @Test
    fun `should throw exception when pending data not found during verification`() {
        val email = "test@example.com"
        val code = "123456"

        `when`(pendingSignUpStore.retrieve(email)).thenReturn(null)

        assertThatThrownBy {
            userSignUpUseCase.verifyEmail(email, code)
        }.isInstanceOf(CustomException.AuthenticationException.ExpiredEmailVerification::class.java)
    }
}
