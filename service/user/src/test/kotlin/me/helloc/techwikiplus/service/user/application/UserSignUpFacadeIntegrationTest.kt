package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.service.user.adapter.outbound.cache.VerificationCodeFakeStore
import me.helloc.techwikiplus.service.user.adapter.outbound.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.adapter.outbound.id.FakeIdGenerator
import me.helloc.techwikiplus.service.user.adapter.outbound.mail.FakeEmailTemplatePrinter
import me.helloc.techwikiplus.service.user.adapter.outbound.messaging.FakeMailSender
import me.helloc.techwikiplus.service.user.adapter.outbound.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.adapter.outbound.security.FakePasswordCipher
import me.helloc.techwikiplus.service.user.application.port.inbound.UserSignUpUseCase
import me.helloc.techwikiplus.service.user.application.service.UserSignUpService
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
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
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordEncoder
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import java.time.Instant

class UserSignUpFacadeIntegrationTest : FunSpec({

    test("회원가입 성공 테스트") {

        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordCipher()

        val writer = UserWriter(repository)
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val auditor =
            Auditor(
                clockHolder = FakeClockHolder(now = now),
            )
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplatePrinter()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpService(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordEncoder = userPasswordEncoder,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given : 올바른 입력값으로 회원가입을 시도한다.
        val email = Email("test@gmail.com")
        val password = RawPassword("Password!123")
        val confirmPassword = RawPassword("Password!123")
        val nickname = Nickname("testUser")

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
        val user: User =
            repository.findBy(email)
                ?: throw IllegalStateException("User not found in repository")
        user.email.value shouldBe email.value
        user.nickname.value shouldBe nickname.value
        user.encodedPassword.value shouldBe passwordEncoder.encode(password).value
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
        val passwordEncoder = FakePasswordCipher()

        val writer = UserWriter(repository)
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplatePrinter()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpService(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordEncoder = userPasswordEncoder,
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
                        email = Email(email),
                        password = RawPassword("NewPassword!123"),
                        confirmPassword = RawPassword("NewPassword!123"),
                        nickname = Nickname("newUser"),
                    ),
                )
            }

        exception.message shouldContain email
    }

    test("비밀번호와 비밀번호 확인이 일치하지 않으면 PasswordMismatchException 발생") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordCipher()

        val writer = UserWriter(repository)
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplatePrinter()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpService(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordEncoder = userPasswordEncoder,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given: 서로 다른 비밀번호 입력
        val email = Email("test@gmail.com")
        val password = RawPassword("Password!123")
        val confirmPassword = RawPassword("DifferentPassword!123")
        val nickname = Nickname("testUser")

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
        repository.findBy(email) shouldBe null
    }

    test("회원가입 시 이메일 발송이 정상적으로 이루어지는지 확인") {
        // 테스트 준비
        val repository = FakeUserRepository()
        val now: Instant = Instant.now()
        val passwordEncoder = FakePasswordCipher()

        val writer = UserWriter(repository)
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val auditor = Auditor(clockHolder = FakeClockHolder(now = now))
        val mailSender = FakeMailSender()
        val userCacheStore = VerificationCodeFakeStore()
        val emailTemplateService = FakeEmailTemplatePrinter()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                userCacheStore,
                emailTemplateService,
            )
        val passwordConfirmationVerifier = PasswordConfirmationVerifier()
        val idGenerator = FakeIdGenerator()

        val sut =
            UserSignUpService(
                idGenerator = idGenerator,
                writer = writer,
                userPasswordEncoder = userPasswordEncoder,
                passwordConfirmationVerifier = passwordConfirmationVerifier,
                auditor = auditor,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // Given
        val email = Email("test@gmail.com")
        val password = RawPassword("Password!123")
        val confirmPassword = RawPassword("Password!123")
        val nickname = Nickname("testUser")

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
        userCacheStore.exists(email) shouldBe true
    }
})
