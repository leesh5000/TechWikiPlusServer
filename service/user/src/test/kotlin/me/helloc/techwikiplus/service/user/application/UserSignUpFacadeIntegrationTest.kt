package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.service.user.domain.exception.EmailValidationException
import me.helloc.techwikiplus.service.user.domain.exception.NicknameValidationException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.PasswordConfirmationVerifier
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import me.helloc.techwikiplus.service.user.infrastructure.cache.VerificationCodeFakeStore
import me.helloc.techwikiplus.service.user.infrastructure.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.id.FakeIdGenerator
import me.helloc.techwikiplus.service.user.infrastructure.mail.FakeEmailTemplateService
import me.helloc.techwikiplus.service.user.infrastructure.messaging.FakeMailSender
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserSignUpUseCase
import java.time.Instant

class UserSignUpFacadeIntegrationTest : FunSpec({

    test("회원가입 성공 테스트") {

        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor =
            Auditor(
                clockHolder = FakeClockHolder(now = now),
            )
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given : 올바른 입력값으로 회원가입을 시도한다.
        val email = "test@gmail.com"
        val password = "Password!123"
        val confirmPassword = "Password!123"
        val nickname = "testUser"

        // When : 회원가입을 시도한다.

        // Then 1 : 회원가입을 시도하면 아무 에러도 발생하지 않아야 한다.
        shouldNotThrow<Throwable> {
            sut.execute(
                UserSignUpUseCase.Command(
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    nickname = nickname,
                ),
            )
        }

        // Then 2 : 저장소에 입력값에 맞는 회원 정보를 가진 회원이 저장되어 있어야하고, 상태가 "PENDING" 이어야 한다.
        val userEmail = Email(email)
        val user: User =
            repository.findBy(userEmail)
                ?: throw IllegalStateException("User not found in repository")
        user.email.value shouldBe email
        user.nickname.value shouldBe nickname
        user.encodedPassword.value shouldBe passwordEncoder.encode(RawPassword(password)).value
        user.createdAt shouldBe now
        user.modifiedAt shouldBe now
        user.status shouldBe UserStatus.PENDING
        user.role shouldBe UserRole.USER

        // Then 3 : 해당 이메일로 인증 코드 메일이 발송된 기록이 있어야한다.
        val isSentTo = userEmailVerificationCodeManager.hasMailBeenSentTo(user)
        isSentTo shouldBe true
    }

    test("중복된 이메일로 회원가입 시 UserAlreadyExistsException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 이미 등록된 사용자 생성
        val email = "existing@gmail.com"
        val encodedPassword = passwordEncoder.encode(RawPassword("OldPassword!123")).value
        val existingUser =
            User(
                id = idGenerator.next(),
                email = Email(email),
                encodedPassword = EncodedPassword(encodedPassword),
                nickname = Nickname("existingUser"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(existingUser)

        // When & Then: 같은 이메일로 회원가입 시도시 예외 발생
        val exception =
            shouldThrow<UserAlreadyExistsException.ForEmail> {
                sut.execute(
                    UserSignUpUseCase.Command(
                        email = email,
                        password = "NewPassword!123",
                        confirmPassword = "NewPassword!123",
                        nickname = "newUser",
                    ),
                )
            }

        exception.message shouldContain email
    }

    test("비밀번호와 비밀번호 확인이 일치하지 않으면 PasswordMismatchException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 서로 다른 비밀번호 입력
        val email = "test@gmail.com"
        val password = "Password!123"
        val confirmPassword = "DifferentPassword!123"
        val nickname = "testUser"

        // When & Then: 예외 발생
        shouldThrow<PasswordMismatchException> {
            sut.execute(
                UserSignUpUseCase.Command(
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    nickname = nickname,
                ),
            )
        }

        // 사용자가 저장되지 않았는지 확인
        repository.findBy(Email(email)) shouldBe null
    }

    test("비밀번호 정책을 만족하지 않으면 PasswordValidationException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 정책을 만족하지 않는 비밀번호 (특수문자 없음)
        val email = "test@gmail.com"
        val password = "Password123"
        val confirmPassword = "Password123"
        val nickname = "testUser"

        // When & Then: 예외 발생
        val exception =
            shouldThrow<PasswordValidationException> {
                sut.execute(
                    UserSignUpUseCase.Command(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    ),
                )
            }

        exception.errorCode shouldBe PasswordValidationException.NO_SPECIAL_CHAR
        exception.message shouldContain "특수문자"

        // 사용자가 저장되지 않았는지 확인
        repository.findBy(Email(email)) shouldBe null
    }

    test("잘못된 이메일 형식으로 회원가입 시 EmailValidationException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 잘못된 이메일 형식
        val email = "invalid-email"
        val password = "Password!123"
        val confirmPassword = "Password!123"
        val nickname = "testUser"

        // When & Then: 예외 발생
        val exception =
            shouldThrow<EmailValidationException> {
                sut.execute(
                    UserSignUpUseCase.Command(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    ),
                )
            }

        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
        exception.message shouldContain "올바른 이메일 형식"
    }

    test("잘못된 닉네임으로 회원가입 시 NicknameValidationException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 빈 닉네임
        val email = "test@gmail.com"
        val password = "Password!123"
        val confirmPassword = "Password!123"
        val nickname = ""

        // When & Then: 예외 발생
        val exception =
            shouldThrow<NicknameValidationException> {
                sut.execute(
                    UserSignUpUseCase.Command(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    ),
                )
            }

        exception.errorCode shouldBe NicknameValidationException.BLANK_NICKNAME
        exception.message shouldContain "닉네임은 필수"
    }

    test("회원가입 시 이메일 발송이 정상적으로 이루어지는지 확인") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordEncoder()

        val writer = UserWriter(repository)
        val userPasswordService = UserPasswordService(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpFacade(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordService = userPasswordService,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given
        val email = "test@gmail.com"
        val password = "Password!123"
        val confirmPassword = "Password!123"
        val nickname = "testUser"

        // When
        sut.execute(
            UserSignUpUseCase.Command(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                nickname = nickname,
            ),
        )

        // Then: 메일 발송 기록 확인
        mailSender.hasMailBeenSentTo(email) shouldBe true

        // Then: 캐시에 인증 코드가 저장되었는지 확인
        userCacheStore.exists(Email(email)) shouldBe true
    }
})
