package me.helloc.techwikiplus.service.user.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeCacheStore
import me.helloc.techwikiplus.service.user.domain.port.FakeClockHolder
import me.helloc.techwikiplus.service.user.domain.port.FakeMailSender
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class UserVerifyFacadeTest : FunSpec({

    lateinit var userVerifyFacade: UserVerifyFacade
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

        userVerifyFacade =
            UserVerifyFacade(
                userReader = userReader,
                emailVerifyService = emailVerifyService,
                userModifier = userModifier,
            )
    }

    test("유효한 인증 코드로 사용자 인증이 성공해야 한다") {
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

        val validCode = RegistrationCode("123456")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        // when
        userVerifyFacade.execute(email, validCode)

        // then
        val activatedUser = fakeUserRepository.findBy(email)
        activatedUser?.status shouldBe UserStatus.ACTIVE
        activatedUser?.isActive() shouldBe true
        activatedUser?.modifiedAt shouldBe fakeClockHolder.now()
    }

    test("잘못된 인증 코드로 인증 시도 시 예외가 발생해야 한다") {
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

        val validCode = RegistrationCode("123456")
        val invalidCode = RegistrationCode("999999")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userVerifyFacade.execute(email, invalidCode)
            }

        exception.errorCode shouldBe ErrorCode.CODE_MISMATCH

        val user = fakeUserRepository.findBy(email)
        user?.status shouldBe UserStatus.PENDING
    }

    test("존재하지 않는 PENDING 사용자의 인증 시도 시 예외가 발생해야 한다") {
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

        val code = RegistrationCode("123456")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userVerifyFacade.execute(email, code)
            }

        exception.errorCode shouldBe ErrorCode.NOT_FOUND_PENDING_USER
        exception.params shouldBe arrayOf(email.value)
    }

    test("인증 코드가 캐시에 없는 경우 예외가 발생해야 한다") {
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

        val code = RegistrationCode("123456")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userVerifyFacade.execute(email, code)
            }

        exception.errorCode shouldBe ErrorCode.REGISTRATION_EXPIRED

        val user = fakeUserRepository.findBy(email)
        user?.status shouldBe UserStatus.PENDING
    }

    test("인증 성공 후 캐시에서 인증 코드가 삭제되어야 한다") {
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

        val validCode = RegistrationCode("123456")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        // when
        userVerifyFacade.execute(email, validCode)

        // then
        fakeCacheStore.contains(cacheKey) shouldBe false
    }

    test("인증 프로세스가 올바른 순서로 실행되어야 한다") {
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

        val validCode = RegistrationCode("123456")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        var userReaderCalled = false
        var emailVerifyCalled = false
        var userModifierCalled = false
        val callOrder = mutableListOf<String>()

        fakeUserRepository =
            object : FakeUserRepository() {
                init {
                    save(pendingUser)
                }

                override fun findBy(email: Email): User? {
                    if (!userReaderCalled) {
                        userReaderCalled = true
                        callOrder.add("userReader")
                    }
                    return super.findBy(email)
                }

                override fun save(user: User): User {
                    if (user.status == UserStatus.ACTIVE) {
                        userModifierCalled = true
                        callOrder.add("userModifier")
                    }
                    return super.save(user)
                }
            }

        fakeCacheStore =
            object : FakeCacheStore() {
                init {
                    put(cacheKey, validCode.value)
                }

                override fun get(key: String): String? {
                    if (!emailVerifyCalled) {
                        emailVerifyCalled = true
                        callOrder.add("emailVerify")
                    }
                    return super.get(key)
                }
            }

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

        userVerifyFacade =
            UserVerifyFacade(
                userReader = userReader,
                emailVerifyService = emailVerifyService,
                userModifier = userModifier,
            )

        // when
        userVerifyFacade.execute(email, validCode)

        // then
        callOrder shouldBe listOf("userReader", "emailVerify", "userModifier")
        userReaderCalled shouldBe true
        emailVerifyCalled shouldBe true
        userModifierCalled shouldBe true
    }

    test("인증 완료 후 사용자의 수정 시간이 업데이트되어야 한다") {
        // given
        val fixedTime = Instant.parse("2025-01-15T10:30:00Z")
        fakeClockHolder.setFixedTime(fixedTime)

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

        val validCode = RegistrationCode("123456")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        // when
        userVerifyFacade.execute(email, validCode)

        // then
        val activatedUser = fakeUserRepository.findBy(email)
        activatedUser?.createdAt shouldBe createdTime
        activatedUser?.modifiedAt shouldBe fixedTime
    }

    test("동일한 사용자가 여러 번 인증 시도 시 첫 번째 인증만 성공해야 한다") {
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

        val validCode = RegistrationCode("123456")
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.put(cacheKey, validCode.value)

        // when
        userVerifyFacade.execute(email, validCode)

        // then
        val exception =
            shouldThrow<DomainException> {
                userVerifyFacade.execute(email, validCode)
            }

        exception.errorCode shouldBe ErrorCode.NOT_FOUND_PENDING_USER
    }

    test("BANNED 상태의 사용자는 인증할 수 없어야 한다") {
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

        val code = RegistrationCode("123456")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                userVerifyFacade.execute(email, code)
            }

        exception.errorCode shouldBe ErrorCode.NOT_FOUND_PENDING_USER
        exception.params shouldBe arrayOf(email.value)
    }
})
