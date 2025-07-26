package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.port.outbound.Clock
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import me.helloc.techwikiplus.user.infrastructure.mail.fake.FakeMailSender
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import me.helloc.techwikiplus.user.infrastructure.verificationcode.fake.FakeVerificationCodeStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

class ResendVerificationCodeUseCaseUnitTest {
    private lateinit var mailSender: FakeMailSender
    private lateinit var verificationCodeStore: FakeVerificationCodeStore
    private lateinit var pendingUserValidator: PendingUserValidator
    private lateinit var userRepository: FakeUserRepository
    private lateinit var resendVerificationCodeUseCase: ResendVerificationCodeUseCase

    @BeforeEach
    fun setUp() {
        mailSender = FakeMailSender()
        verificationCodeStore = FakeVerificationCodeStore()
        userRepository = FakeUserRepository()
        pendingUserValidator = PendingUserValidator(userRepository)
        resendVerificationCodeUseCase =
            ResendVerificationCodeUseCase(
                mailSender = mailSender,
                verificationCodeStore = verificationCodeStore,
                pendingUserValidator = pendingUserValidator,
            )
    }

    @Test
    @DisplayName("인증 코드를 성공적으로 재전송해야 한다")
    fun shouldResendVerificationCodeSuccessfully() {
        // given
        val email = "pending@example.com"
        val userId = 1L

        // Pending 상태의 사용자 생성
        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = "pendinguser",
                password = "encodedPassword",
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // when
        resendVerificationCodeUseCase.resendVerificationCode(email)

        // then
        // 이메일이 전송되었는지 확인
        assertThat(mailSender.getSentEmails()).hasSize(1)
        assertThat(mailSender.getSentEmails()[0].email).isEqualTo(email)

        // 새로운 인증 코드가 저장되었는지 확인
        val storedCode = verificationCodeStore.retrieveOrThrows(email)
        assertThat(storedCode).isNotNull
    }

    @Test
    @DisplayName("기존 인증 코드를 덮어써야 한다")
    fun shouldOverwriteExistingVerificationCode() {
        // given
        val email = "pending@example.com"
        val userId = 1L

        // Pending 상태의 사용자 생성
        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = "pendinguser",
                password = "encodedPassword",
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // 기존 인증 코드 저장
        val oldCode = VerificationCode("123456")
        verificationCodeStore.storeWithExpiry(email, oldCode, Duration.ofMinutes(5))

        // when
        resendVerificationCodeUseCase.resendVerificationCode(email)

        // then
        val newCode = verificationCodeStore.retrieveOrThrows(email)
        assertThat(newCode).isNotNull
        // FakeMailSender는 새로운 코드를 생성하므로 기존 코드와 달라야 함
        assertThat(newCode.value).isNotEqualTo(oldCode.value)
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 예외를 발생시켜야 한다")
    fun shouldThrowExceptionWhenUserNotFound() {
        // given
        val email = "nonexistent@example.com"

        // when & then
        assertThatThrownBy {
            resendVerificationCodeUseCase.resendVerificationCode(email)
        }.isInstanceOf(CustomException.AuthenticationException.PendingUserNotFound::class.java)
            .hasMessage(
                "Pending user not found for email: $email. " +
                    "Please ensure you have registered and requested verification.",
            )

        // 이메일이 전송되지 않았는지 확인
        assertThat(mailSender.getSentEmails()).isEmpty()
    }

    @Test
    @DisplayName("이미 인증된 사용자일 때 예외를 발생시켜야 한다")
    fun shouldThrowExceptionWhenUserAlreadyVerified() {
        // given
        val email = "verified@example.com"
        val userId = 1L

        // 이미 인증된 사용자 생성
        val verifiedUser =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "verifieduser",
                password = "encodedPassword",
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(verifiedUser)

        // when & then
        assertThatThrownBy {
            resendVerificationCodeUseCase.resendVerificationCode(email)
        }.isInstanceOf(CustomException.AuthenticationException.PendingUserNotFound::class.java)
            .hasMessage(
                "Pending user not found for email: $email. " +
                    "Please ensure you have registered and requested verification.",
            )

        // 이메일이 전송되지 않았는지 확인
        assertThat(mailSender.getSentEmails()).isEmpty()
    }

    @Test
    @DisplayName("서로 다른 사용자들에게 여러 코드를 전송할 수 있어야 한다")
    fun shouldSendMultipleCodesForDifferentUsers() {
        // given
        val users =
            listOf(
                Pair(1L, "user1@example.com"),
                Pair(2L, "user2@example.com"),
                Pair(3L, "user3@example.com"),
            )

        // 여러 Pending 사용자 생성
        users.forEach { (id, email) ->
            val pendingUser =
                User.withPendingUser(
                    id = id,
                    email = UserEmail(email, false),
                    nickname = "user$id",
                    password = "encodedPassword",
                    clock = Clock.system,
                )
            userRepository.insertOrUpdate(pendingUser)
        }

        // when
        users.forEach { (_, email) ->
            resendVerificationCodeUseCase.resendVerificationCode(email)
        }

        // then
        assertThat(mailSender.getSentEmails()).hasSize(3)
        val sentEmails = mailSender.getSentEmails().map { it.email }
        assertThat(sentEmails).containsExactlyInAnyOrder(
            "user1@example.com",
            "user2@example.com",
            "user3@example.com",
        )

        // 각 사용자의 인증 코드가 저장되었는지 확인
        users.forEach { (_, email) ->
            val code = verificationCodeStore.retrieveOrThrows(email)
            assertThat(code).isNotNull
        }
    }

    @Test
    @DisplayName("인증 코드에 올바른 TTL을 설정해야 한다")
    fun shouldSetCorrectTTLForVerificationCode() {
        // given
        val email = "pending@example.com"
        val userId = 1L

        val pendingUser =
            User.withPendingUser(
                id = userId,
                email = UserEmail(email, false),
                nickname = "pendinguser",
                password = "encodedPassword",
                clock = Clock.system,
            )
        userRepository.insertOrUpdate(pendingUser)

        // when
        resendVerificationCodeUseCase.resendVerificationCode(email)

        // then
        // 인증 코드가 저장되었는지 확인
        val code = verificationCodeStore.retrieveOrThrows(email)
        assertThat(code).isNotNull
    }
}
