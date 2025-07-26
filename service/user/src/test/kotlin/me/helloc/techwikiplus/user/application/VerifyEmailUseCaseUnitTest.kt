package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.authentication.ExpiredEmailVerificationException
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidVerificationCodeException
import me.helloc.techwikiplus.user.domain.exception.notfound.UserEmailNotFoundException
import me.helloc.techwikiplus.user.domain.exception.validation.AlreadyVerifiedEmailException
import me.helloc.techwikiplus.user.domain.port.outbound.Clock
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import me.helloc.techwikiplus.user.infrastructure.verificationcode.fake.FakeVerificationCodeStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

class VerifyEmailUseCaseUnitTest {
    private lateinit var verificationCodeStore: FakeVerificationCodeStore
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userReader: UserReader
    private lateinit var userWriter: UserWriter
    private lateinit var verifyEmailUseCase: VerifyEmailUseCase

    @BeforeEach
    fun setUp() {
        verificationCodeStore = FakeVerificationCodeStore()
        userRepository = FakeUserRepository()
        userReader = UserReader(userRepository)
        userWriter = UserWriter(userRepository)
        verifyEmailUseCase =
            VerifyEmailUseCase(
                verificationCodeStore = verificationCodeStore,
                userReader = userReader,
                userWriter = userWriter,
            )
    }

    @Test
    @DisplayName("인증에 성공하면 사용자 상태를 활성화로 변경")
    fun shouldVerifySuccessfully() {
        // given
        val email = "test@example.com"
        val code = "123456"
        val userId = 1L

        // Pending 상태의 사용자 생성
        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = "testuser",
                password = "encodedPassword",
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // 인증 코드 저장
        val verificationCode = VerificationCode(code)
        verificationCodeStore.storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))

        // when
        verifyEmailUseCase.verify(email, code)

        // then
        val verifiedUser = userRepository.findByEmail(email)
        assertThat(verifiedUser).isNotNull
        assertThat(verifiedUser!!.isPending()).isFalse()
        assertThat(verifiedUser.status).isEqualTo(UserStatus.ACTIVE)
        assertThat(verifiedUser.email.verified).isTrue()
    }

    @Test
    @DisplayName("인증 코드를 찾을 수 없을 때 예외 발생")
    fun shouldThrowExceptionWhenVerificationCodeNotFound() {
        // given
        val email = "test@example.com"
        val code = "123456"

        // 인증 코드를 저장하지 않음

        // when & then
        assertThatThrownBy {
            verifyEmailUseCase.verify(email, code)
        }.isInstanceOf(ExpiredEmailVerificationException::class.java)
            .hasMessage(
                "Email verification expired. Details: Email verification expired for email: $email. " +
                    "Please request a new verification code.",
            )
    }

    @Test
    @DisplayName("인증 코드가 틀렸을 때 예외 발생")
    fun shouldThrowExceptionWhenVerificationCodeIsIncorrect() {
        // given
        val email = "test@example.com"
        val correctCode = "123456"
        val wrongCode = "654321"

        // 인증 코드 저장
        val verificationCode = VerificationCode(correctCode)
        verificationCodeStore.storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))

        // when & then
        assertThatThrownBy {
            verifyEmailUseCase.verify(email, wrongCode)
        }.isInstanceOf(InvalidVerificationCodeException::class.java)
            .hasMessage(
                "Invalid verification code. Details: Invalid verification code: $wrongCode. " +
                    "Please check the code and try again.",
            )
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 예외 발생")
    fun shouldThrowExceptionWhenUserNotFound() {
        // given
        val email = "nonexistent@example.com"
        val code = "123456"

        // 인증 코드만 저장하고 사용자는 생성하지 않음
        val verificationCode = VerificationCode(code)
        verificationCodeStore.storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))

        // when & then
        assertThatThrownBy {
            verifyEmailUseCase.verify(email, code)
        }.isInstanceOf(UserEmailNotFoundException::class.java)
            .hasMessage("User not found. Details: User not found with email: $email")
    }

    @Test
    @DisplayName("이미 인증된 사용자일 때 예외 발생")
    fun shouldThrowExceptionWhenUserAlreadyVerified() {
        // given
        val email = "test@example.com"
        val code = "123456"
        val userId = 1L

        // 이미 인증된 사용자 생성
        val verifiedUser =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = "encodedPassword",
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(verifiedUser)

        // 인증 코드 저장
        val verificationCode = VerificationCode(code)
        verificationCodeStore.storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))

        // when & then
        assertThatThrownBy {
            verifyEmailUseCase.verify(email, code)
        }.isInstanceOf(AlreadyVerifiedEmailException::class.java)
            .hasMessage("Email is already verified. Details: Your input: $email")
    }

    @Test
    @DisplayName("인증 후 사용자 정보 유지")
    fun shouldMaintainUserInformationAfterVerification() {
        // given
        val email = "test@example.com"
        val code = "123456"
        val userId = 1L
        val nickname = "testuser"
        val password = "encodedPassword"

        // Pending 상태의 사용자 생성
        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = nickname,
                password = password,
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // 인증 코드 저장
        val verificationCode = VerificationCode(code)
        verificationCodeStore.storeWithExpiry(email, verificationCode, Duration.ofMinutes(5))

        // when
        verifyEmailUseCase.verify(email, code)

        // then
        val verifiedUser = userRepository.findByEmail(email)
        assertThat(verifiedUser).isNotNull
        assertThat(verifiedUser!!.id).isEqualTo(userId)
        assertThat(verifiedUser.getEmailValue()).isEqualTo(email)
        assertThat(verifiedUser.nickname).isEqualTo(nickname)
        assertThat(verifiedUser.password).isEqualTo(password)
    }
}
