package me.helloc.techwikiplus.service.user.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeCacheStore
import me.helloc.techwikiplus.service.user.domain.port.FakeClockHolder
import me.helloc.techwikiplus.service.user.domain.port.FakeIdGenerator
import me.helloc.techwikiplus.service.user.domain.port.FakeMailSender
import me.helloc.techwikiplus.service.user.domain.port.FakePasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserRegister
import java.time.Instant

class UserSignUpFacadeTest : FunSpec({

    lateinit var userSignUpFacade: UserSignUpFacade
    lateinit var userRegister: UserRegister
    lateinit var emailVerifyService: EmailVerifyService
    lateinit var userModifier: UserModifier

    lateinit var fakeUserRepository: FakeUserRepository
    lateinit var fakeClockHolder: FakeClockHolder
    lateinit var fakeIdGenerator: FakeIdGenerator
    lateinit var fakePasswordEncryptor: FakePasswordEncryptor
    lateinit var fakeMailSender: FakeMailSender
    lateinit var fakeCacheStore: FakeCacheStore

    beforeEach {
        fakeUserRepository = FakeUserRepository()
        fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
        fakeIdGenerator = FakeIdGenerator()
        fakePasswordEncryptor = FakePasswordEncryptor()
        fakeMailSender = FakeMailSender()
        fakeCacheStore = FakeCacheStore()

        userRegister =
            UserRegister(
                clockHolder = fakeClockHolder,
                idGenerator = fakeIdGenerator,
                repository = fakeUserRepository,
                passwordEncryptor = fakePasswordEncryptor,
            )

        emailVerifyService =
            EmailVerifyService(
                mailSender = fakeMailSender,
                cacheStore = fakeCacheStore,
            )

        userModifier =
            UserModifier(
                clockHolder = fakeClockHolder,
                repository = fakeUserRepository,
            )

        userSignUpFacade =
            UserSignUpFacade(
                userRegister = userRegister,
                emailVerifyService = emailVerifyService,
                userModifier = userModifier,
            )
    }

    test("유효한 회원가입 정보로 성공적으로 회원가입이 완료되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUsers = fakeUserRepository.getAll()
        savedUsers.size shouldBe 1

        val savedUser = savedUsers[0]
        savedUser.email shouldBe email
        savedUser.nickname shouldBe nickname
        savedUser.status shouldBe UserStatus.PENDING
        savedUser.role shouldBe UserRole.USER
        savedUser.id.value shouldBe 1000000L

        val expectedEncodedPassword = fakePasswordEncryptor.encode(password)
        savedUser.encodedPassword shouldBe expectedEncodedPassword
    }

    test("회원가입 시 이메일 인증 코드가 발송되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        fakeMailSender.getSentMailCount() shouldBe 1
        fakeMailSender.wasSentTo(email) shouldBe true

        val sentMail = fakeMailSender.getLastSentMail()
        sentMail shouldNotBe null
        sentMail!!.to shouldBe email
        sentMail.content.subject shouldBe "TechWiki+ 회원가입 인증 코드"
        sentMail.content.body shouldNotBe null
    }

    test("회원가입 시 인증 코드가 캐시에 저장되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.contains(cacheKey) shouldBe true

        val storedCode = fakeCacheStore.get(cacheKey)
        storedCode shouldNotBe null
        storedCode!!.length shouldBe 6
        storedCode.all { it.isDigit() } shouldBe true
    }

    test("회원가입 후 사용자 상태가 PENDING으로 설정되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUser = fakeUserRepository.findBy(email)
        savedUser shouldNotBe null
        savedUser!!.status shouldBe UserStatus.PENDING
        savedUser.isPending() shouldBe true
    }

    test("비밀번호와 비밀번호 확인이 일치하지 않으면 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("DifferentPassword123!")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userSignUpFacade.execute(
                    email = email,
                    nickname = nickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.errorCode shouldBe ErrorCode.PASSWORD_MISMATCH
        fakeUserRepository.getAll().size shouldBe 0
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("이미 존재하는 이메일로 회원가입 시도 시 예외가 발생해야 한다") {
        // given
        val existingEmail = Email("existing@example.com")
        val existingUser =
            User.create(
                id = UserId(2000001L),
                email = existingEmail,
                nickname = Nickname("existinguser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
            )
        fakeUserRepository.save(existingUser)

        val nickname = Nickname("newuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userSignUpFacade.execute(
                    email = existingEmail,
                    nickname = nickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.errorCode shouldBe ErrorCode.DUPLICATE_EMAIL
        exception.params shouldBe arrayOf(existingEmail.value)
        fakeUserRepository.getAll().size shouldBe 1
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("이미 존재하는 닉네임으로 회원가입 시도 시 예외가 발생해야 한다") {
        // given
        val existingNickname = Nickname("existinguser")
        val existingUser =
            User.create(
                id = UserId(2000001L),
                email = Email("existing@example.com"),
                nickname = existingNickname,
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
            )
        fakeUserRepository.save(existingUser)

        val email = Email("new@example.com")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userSignUpFacade.execute(
                    email = email,
                    nickname = existingNickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.errorCode shouldBe ErrorCode.DUPLICATE_NICKNAME
        exception.params shouldBe arrayOf(existingNickname.value)
        fakeUserRepository.getAll().size shouldBe 1
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("회원가입 프로세스가 올바른 순서로 실행되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        var userRegisterCalled = false
        var emailVerifyCalled = false
        var userModifierCalled = false
        var callOrder = mutableListOf<String>()

        // 각 단계에서 호출 순서를 기록하기 위한 검증
        fakeUserRepository =
            object : FakeUserRepository() {
                override fun save(user: User): User {
                    if (!userRegisterCalled && user.status == UserStatus.ACTIVE) {
                        userRegisterCalled = true
                        callOrder.add("userRegister")
                    } else if (user.status == UserStatus.PENDING) {
                        userModifierCalled = true
                        callOrder.add("userModifier")
                    }
                    return super.save(user)
                }
            }

        fakeMailSender =
            object : FakeMailSender() {
                override fun send(
                    to: Email,
                    content: me.helloc.techwikiplus.service.user.domain.model.MailContent,
                ) {
                    emailVerifyCalled = true
                    callOrder.add("emailVerify")
                    super.send(to, content)
                }
            }

        userRegister =
            UserRegister(
                clockHolder = fakeClockHolder,
                idGenerator = fakeIdGenerator,
                repository = fakeUserRepository,
                passwordEncryptor = fakePasswordEncryptor,
            )

        emailVerifyService =
            EmailVerifyService(
                mailSender = fakeMailSender,
                cacheStore = fakeCacheStore,
            )

        userModifier =
            UserModifier(
                clockHolder = fakeClockHolder,
                repository = fakeUserRepository,
            )

        userSignUpFacade =
            UserSignUpFacade(
                userRegister = userRegister,
                emailVerifyService = emailVerifyService,
                userModifier = userModifier,
            )

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        callOrder shouldBe listOf("userRegister", "emailVerify", "userModifier")
        userRegisterCalled shouldBe true
        emailVerifyCalled shouldBe true
        userModifierCalled shouldBe true
    }

    test("회원가입 시 생성 시간과 수정 시간이 올바르게 설정되어야 한다") {
        // given
        val fixedTime = Instant.parse("2025-01-15T10:30:00Z")
        fakeClockHolder.setFixedTime(fixedTime)

        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.execute(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUser = fakeUserRepository.findBy(email)
        savedUser shouldNotBe null
        savedUser!!.createdAt shouldBe fixedTime
        savedUser.modifiedAt shouldBe fixedTime
    }

    test("동시에 여러 회원가입이 발생해도 각각 고유한 ID를 가져야 한다") {
        // given
        val users =
            listOf(
                Triple(Email("user1@example.com"), Nickname("user1"), RawPassword("Password1!")),
                Triple(Email("user2@example.com"), Nickname("user2"), RawPassword("Password2!")),
                Triple(Email("user3@example.com"), Nickname("user3"), RawPassword("Password3!")),
            )

        // when
        users.forEach { (email, nickname, password) ->
            userSignUpFacade.execute(
                email = email,
                nickname = nickname,
                password = password,
                confirmPassword = password,
            )
        }

        // then
        val savedUsers = fakeUserRepository.getAll()
        savedUsers.size shouldBe 3

        val userIds = savedUsers.map { it.id.value }
        userIds shouldBe listOf(1000000L, 1000001L, 1000002L)

        userIds.distinct().size shouldBe userIds.size
    }
})
