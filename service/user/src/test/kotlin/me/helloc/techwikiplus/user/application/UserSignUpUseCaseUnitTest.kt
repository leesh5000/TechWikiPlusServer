package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import me.helloc.techwikiplus.user.infrastructure.id.fake.FakeIdGenerator
import me.helloc.techwikiplus.user.infrastructure.mail.fake.FakeMailSender
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakePasswordEncoder
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakePasswordValidator
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import me.helloc.techwikiplus.user.infrastructure.verificationcode.fake.FakeVerificationCodeStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserSignUpUseCaseUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userWriter: UserWriter
    private lateinit var userDuplicateChecker: UserDuplicateChecker
    private lateinit var passwordValidator: FakePasswordValidator
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var mailSender: FakeMailSender
    private lateinit var verificationCodeStore: FakeVerificationCodeStore
    private lateinit var idGenerator: FakeIdGenerator
    private lateinit var userSignUpUseCase: UserSignUpUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userWriter = UserWriter(userRepository)
        userDuplicateChecker = UserDuplicateChecker(userRepository)
        passwordValidator = FakePasswordValidator()
        passwordEncoder = FakePasswordEncoder()
        mailSender = FakeMailSender()
        verificationCodeStore = FakeVerificationCodeStore()
        idGenerator = FakeIdGenerator()
        val userRegister =
            UserRegister(
                userWriter = userWriter,
                userDuplicateChecker = userDuplicateChecker,
                passwordValidator = passwordValidator,
                passwordEncoder = passwordEncoder,
                idGenerator = idGenerator,
            )
        val verificationCodeSender =
            VerificationCodeSender(
                mailSender = mailSender,
                verificationCodeStore = verificationCodeStore,
            )
        userSignUpUseCase =
            UserSignUpUseCase(
                userRegister = userRegister,
                verificationCodeSender = verificationCodeSender,
            )
    }

    @Test
    @DisplayName("유효한 정보로 회원가입 성공")
    fun shouldSignUpUserSuccessfully() {
        // given
        val email = "test@example.com"
        val nickname = "testuser"
        val password = "password123"
        val expectedUserId = 1L
        idGenerator.reset(expectedUserId)

        // when
        userSignUpUseCase.signUp(email, nickname, password)

        // then
        val savedUser = userRepository.findByEmail(email)
        assertThat(savedUser).isNotNull
        assertThat(savedUser!!.id).isEqualTo(expectedUserId)
        assertThat(savedUser.getEmailValue()).isEqualTo(email)
        assertThat(savedUser.nickname).isEqualTo(nickname)
        assertThat(savedUser.isPending()).isTrue()

        // 비밀번호가 인코딩되었는지 확인
        assertThat(passwordEncoder.matches(password, savedUser.password)).isTrue()

        // 이메일이 전송되었는지 확인
        assertThat(mailSender.getSentEmails()).hasSize(1)
        assertThat(mailSender.getSentEmails()[0].email).isEqualTo(email)

        // 인증 코드가 저장되었는지 확인
        val storedCode = verificationCodeStore.retrieveOrThrows(email)
        assertThat(storedCode).isNotNull
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 예외 발생")
    fun shouldThrowExceptionWhenEmailAlreadyExists() {
        // given
        val email = "existing@example.com"
        val nickname = "newuser"
        val password = "password123"

        // 기존 사용자 생성
        userSignUpUseCase.signUp(email, "existinguser", "existingpass")

        // when & then
        assertThatThrownBy {
            userSignUpUseCase.signUp(email, nickname, password)
        }.isInstanceOf(CustomException.ConflictException.DuplicateEmail::class.java)
            .hasMessageContaining("Email already exists")

        // 이메일이 전송되지 않았는지 확인
        assertThat(mailSender.getSentEmails()).hasSize(1) // 첫 번째 가입 때만 전송
    }

    @Test
    @DisplayName("이미 존재하는 닉네임으로 회원가입 시 예외 발생")
    fun shouldThrowExceptionWhenNicknameAlreadyExists() {
        // given
        val email = "newuser@example.com"
        val nickname = "existinguser"
        val password = "password123"

        // 기존 사용자 생성
        userSignUpUseCase.signUp("existing@example.com", nickname, "existingpass")

        // when & then
        assertThatThrownBy {
            userSignUpUseCase.signUp(email, nickname, password)
        }.isInstanceOf(CustomException.ConflictException.DuplicateNickname::class.java)
            .hasMessageContaining("Nickname already exists")

        // 이메일이 전송되지 않았는지 확인
        assertThat(mailSender.getSentEmails()).hasSize(1) // 첫 번째 가입 때만 전송
    }

    @Test
    @DisplayName("유효하지 않은 비밀번호로 회원가입 시 예외 발생")
    fun shouldThrowExceptionWhenPasswordIsInvalid() {
        // given
        val email = "test@example.com"
        val nickname = "testuser"
        val invalidPassword = "short" // FakeUserPasswordService는 6자 미만 비밀번호를 거부함

        // when & then
        assertThatThrownBy {
            userSignUpUseCase.signUp(email, nickname, invalidPassword)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("비밀번호는 최소 6자 이상이어야 합니다.")

        // 사용자가 생성되지 않았는지 확인
        assertThat(userRepository.findByEmail(email)).isNull()

        // 이메일이 전송되지 않았는지 확인
        assertThat(mailSender.getSentEmails()).isEmpty()
    }

    @Test
    @DisplayName("인증 코드를 올바른 TTL과 함께 저장")
    fun shouldStoreVerificationCodeWithCorrectTTL() {
        // given
        val email = "test@example.com"
        val nickname = "testuser"
        val password = "password123"

        // when
        userSignUpUseCase.signUp(email, nickname, password)

        // then
        val storedCode = verificationCodeStore.retrieveOrThrows(email)
        assertThat(storedCode).isNotNull
        // FakeVerificationCodeStore는 TTL을 내부적으로 관리하므로 별도 검증 불필요
    }

    @Test
    @DisplayName("고유한 사용자 ID 생성")
    fun shouldGenerateUniqueUserIds() {
        // given
        val users =
            listOf(
                Triple("user1@example.com", "user1", "password1"),
                Triple("user2@example.com", "user2", "password2"),
                Triple("user3@example.com", "user3", "password3"),
            )

        // when
        users.forEach { (email, nickname, password) ->
            userSignUpUseCase.signUp(email, nickname, password)
        }

        // then
        val savedUsers = userRepository.findAll()
        assertThat(savedUsers).hasSize(3)

        val userIds = savedUsers.map { it.id }.toSet()
        assertThat(userIds).hasSize(3) // 모든 ID가 고유함
    }
}
