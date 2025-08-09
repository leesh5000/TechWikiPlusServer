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
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeCacheStore
import me.helloc.techwikiplus.service.user.domain.port.FakeClockHolder
import me.helloc.techwikiplus.service.user.domain.port.FakeMailSender
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class UserVerifyResendFacadeTest : FunSpec({

    lateinit var userVerifyResendFacade: UserVerifyResendFacade
    lateinit var userReader: UserReader
    lateinit var emailVerifyService: EmailVerifyService
    lateinit var userModifier: UserModifier

    lateinit var fakeUserRepository: FakeUserRepository
    lateinit var fakeClockHolder: FakeClockHolder
    lateinit var fakeMailSender: FakeMailSender
    lateinit var fakeCacheStore: FakeCacheStore

    beforeEach {
        fakeUserRepository = FakeUserRepository()
        fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
        fakeMailSender = FakeMailSender()
        fakeCacheStore = FakeCacheStore()

        userReader = UserReader(repository = fakeUserRepository)

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

        userVerifyResendFacade =
            UserVerifyResendFacade(
                userReader = userReader,
                emailVerifyService = emailVerifyService,
            )
    }

    afterEach {
        fakeUserRepository.clear()
        fakeMailSender.clear()
        fakeCacheStore.clear()
    }

    context("execute 메서드는") {
        test("PENDING 상태의 사용자에게 인증 메일을 재전송한다") {
            // given
            val email = Email("test@example.com")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(pendingUser)

            // when
            userVerifyResendFacade.execute(email)

            // then
            fakeMailSender.getSentMailCount() shouldBe 1
            fakeMailSender.wasSentTo(email) shouldBe true

            val sentMail = fakeMailSender.getLastSentMail()
            sentMail shouldNotBe null
            sentMail!!.to shouldBe email
            sentMail.content.subject shouldBe "TechWiki+ 회원가입 인증 코드"
        }

        test("재전송 시 새로운 인증 코드가 캐시에 저장된다") {
            // given
            val email = Email("test@example.com")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(pendingUser)

            // 기존 인증 코드가 있는 경우
            val oldCacheKey = "registration_code:${email.value}"
            fakeCacheStore.put(oldCacheKey, "000000")

            // when
            userVerifyResendFacade.execute(email)

            // then
            val cacheKey = "registration_code:${email.value}"
            fakeCacheStore.contains(cacheKey) shouldBe true

            val storedCode = fakeCacheStore.get(cacheKey)
            storedCode shouldNotBe null
            storedCode!!.length shouldBe 6
            storedCode.all { it.isDigit() } shouldBe true
            storedCode shouldNotBe "000000" // 새로운 코드가 생성되어야 함
        }

        test("재전송 후 사용자는 여전히 PENDING 상태를 유지한다") {
            // given
            val email = Email("test@example.com")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(pendingUser)

            // when
            userVerifyResendFacade.execute(email)

            // then
            val updatedUser = fakeUserRepository.findBy(email)
            updatedUser shouldNotBe null
            updatedUser!!.status shouldBe UserStatus.PENDING
            updatedUser.isPending() shouldBe true
        }

        test("재전송 시 PENDING 상태가 유지되므로 수정 시간은 변경되지 않는다") {
            // given
            val email = Email("test@example.com")
            val createdTime = Instant.parse("2024-12-01T00:00:00Z")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = createdTime,
                    modifiedAt = createdTime,
                )
            fakeUserRepository.save(pendingUser)

            val resendTime = Instant.parse("2025-01-15T10:30:00Z")
            fakeClockHolder.setFixedTime(resendTime)

            // when
            userVerifyResendFacade.execute(email)

            // then
            val updatedUser = fakeUserRepository.findBy(email)
            updatedUser shouldNotBe null
            updatedUser!!.createdAt shouldBe createdTime
            // User.setPending()은 이미 PENDING 상태인 경우 동일한 객체를 반환하므로
            // modifiedAt은 변경되지 않음
            updatedUser.modifiedAt shouldBe createdTime
        }

        test("ACTIVE 상태의 사용자로 재전송 시도 시 NO_STATUS_USER 예외가 발생한다") {
            // given
            val email = Email("test@example.com")
            val activeUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(activeUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userVerifyResendFacade.execute(email)
                }

            exception.errorCode shouldBe ErrorCode.NO_STATUS_USER
            exception.params shouldBe arrayOf(UserStatus.PENDING)

            // 메일이 전송되지 않아야 함
            fakeMailSender.getSentMailCount() shouldBe 0
        }

        test("존재하지 않는 이메일로 재전송 시도 시 USER_NOT_FOUND 예외가 발생한다") {
            // given
            val email = Email("nonexistent@example.com")

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userVerifyResendFacade.execute(email)
                }

            exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            exception.params shouldBe arrayOf(email.value)

            // 메일이 전송되지 않아야 함
            fakeMailSender.getSentMailCount() shouldBe 0
        }

        test("여러 번 재전송을 요청해도 매번 새로운 인증 코드가 생성된다") {
            // given
            val email = Email("test@example.com")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(pendingUser)

            val cacheKey = "registration_code:${email.value}"
            val codes = mutableSetOf<String>()

            // when
            repeat(5) {
                userVerifyResendFacade.execute(email)
                val code = fakeCacheStore.get(cacheKey)
                code shouldNotBe null
                codes.add(code!!)

                // 시간 경과 시뮬레이션
                fakeClockHolder.advanceTimeBySeconds(10)
            }

            // then
            codes.size shouldBe 5 // 모든 코드가 고유해야 함
            fakeMailSender.getSentMailCount() shouldBe 5
        }

        test("BANNED 상태의 사용자로 재전송 시도 시 NO_STATUS_USER 예외가 발생한다") {
            // given
            val email = Email("test@example.com")
            val bannedUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.BANNED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(bannedUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userVerifyResendFacade.execute(email)
                }

            exception.errorCode shouldBe ErrorCode.NO_STATUS_USER
            exception.params shouldBe arrayOf(UserStatus.PENDING)
        }

        test("DORMANT 상태의 사용자로 재전송 시도 시 NO_STATUS_USER 예외가 발생한다") {
            // given
            val email = Email("test@example.com")
            val dormantUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(dormantUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userVerifyResendFacade.execute(email)
                }

            exception.errorCode shouldBe ErrorCode.NO_STATUS_USER
            exception.params shouldBe arrayOf(UserStatus.PENDING)
        }

        test("재전송 프로세스가 올바른 순서로 실행되어야 한다") {
            // given
            val email = Email("test@example.com")
            val pendingUser =
                User.create(
                    id = UserId("test-user-1"),
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                )
            fakeUserRepository.save(pendingUser)

            var userReaderCalled = false
            var emailVerifyCalled = false
            var userModifierCalled = false
            val callOrder = mutableListOf<String>()

            fakeUserRepository =
                object : FakeUserRepository() {
                    init {
                        save(pendingUser)
                    }

                    override fun save(user: User): User {
                        if (user.status == UserStatus.PENDING && userReaderCalled && !userModifierCalled) {
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

            userReader = UserReader(repository = fakeUserRepository)

            emailVerifyService =
                EmailVerifyService(
                    mailSender = fakeMailSender,
                    cacheStore = fakeCacheStore,
                )

            userVerifyResendFacade =
                UserVerifyResendFacade(
                    userReader = userReader,
                    emailVerifyService = emailVerifyService,
                )

            // when
            userVerifyResendFacade.execute(email)

            // then
            callOrder shouldBe listOf("userReader", "emailVerify", "userModifier")
            userReaderCalled shouldBe true
            emailVerifyCalled shouldBe true
            userModifierCalled shouldBe true
        }
    }
})
