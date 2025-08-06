package me.helloc.techwikiplus.service.user.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.service.user.adapter.outbound.cache.FakeCacheStore
import me.helloc.techwikiplus.service.user.adapter.outbound.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.adapter.outbound.id.FakeIdGenerator
import me.helloc.techwikiplus.service.user.adapter.outbound.mail.FakeEmailSender
import me.helloc.techwikiplus.service.user.adapter.outbound.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.adapter.outbound.security.FakePasswordCipher
import me.helloc.techwikiplus.service.user.application.service.UserSignUpService
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserSignUpUseCase
import java.time.Instant

class UserSignUpServiceTest : FunSpec({

    lateinit var clockHolder: FakeClockHolder
    lateinit var idGenerator: FakeIdGenerator
    lateinit var repository: FakeUserRepository
    lateinit var passwordCipher: FakePasswordCipher
    lateinit var emailSender: FakeEmailSender
    lateinit var cacheStore: FakeCacheStore
    lateinit var service: UserSignUpService

    beforeEach {
        clockHolder = FakeClockHolder()
        idGenerator = FakeIdGenerator()
        repository = FakeUserRepository()
        passwordCipher = FakePasswordCipher()
        emailSender = FakeEmailSender()
        cacheStore = FakeCacheStore()

        service =
            UserSignUpService(
                clockHolder = clockHolder,
                idGenerator = idGenerator,
                repository = repository,
                passwordCipher = passwordCipher,
                emailSender = emailSender,
                cacheStore = cacheStore,
            )
    }

    afterEach {
        repository.clear()
        emailSender.clear()
        cacheStore.clear()
    }

    context("execute 메서드") {

        test("비밀번호와 확인 비밀번호가 다르면 PasswordsDoNotMatchException을 던진다") {
            // Given
            val command =
                UserSignUpUseCase.Command(
                    email = Email("test@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("DifferentPassword456!"),
                    nickname = Nickname("testuser"),
                )

            // When & Then
            val exception =
                shouldThrow<UserSignUpUseCase.PasswordsDoNotMatchException> {
                    service.execute(command)
                }

            exception.message shouldBe "비밀번호와 비밀번호 확인이 일치하지 않습니다."

            // 사용자가 생성되지 않았는지 확인
            repository.exists(Email("test@example.com")) shouldBe false
            // 이메일이 발송되지 않았는지 확인
            emailSender.getSentMailCount() shouldBe 0
            // 캐시에 저장되지 않았는지 확인
            cacheStore.size() shouldBe 0
        }

        test("이미 존재하는 이메일로 가입 시도하면 UserAlreadyExistsException을 던진다") {
            // Given
            val existingUser =
                User.Companion.create(
                    id = "existing-user-id",
                    email = Email("existing@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword"),
                    nickname = Nickname("existinguser"),
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
            repository.insert(existingUser)

            val command =
                UserSignUpUseCase.Command(
                    email = Email("existing@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("Password123!"),
                    nickname = Nickname("newuser"),
                )

            // When & Then
            val exception =
                shouldThrow<UserSignUpUseCase.UserAlreadyExistsException> {
                    service.execute(command)
                }

            exception.message shouldContain "이메일 'existing@example.com'"
            exception.message shouldContain "이미 사용 중입니다"

            // 이메일이 발송되지 않았는지 확인
            emailSender.getSentMailCount() shouldBe 0
            // 캐시에 저장되지 않았는지 확인
            cacheStore.size() shouldBe 0
        }

        test("이미 존재하는 닉네임으로 가입 시도하면 UserAlreadyExistsException을 던진다") {
            // Given
            val existingUser =
                User.Companion.create(
                    id = "existing-user-id",
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword"),
                    nickname = Nickname("existingNickname"),
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
            repository.insert(existingUser)

            val command =
                UserSignUpUseCase.Command(
                    email = Email("newuser@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("Password123!"),
                    nickname = Nickname("existingNickname"),
                )

            // When & Then
            val exception =
                shouldThrow<UserSignUpUseCase.UserAlreadyExistsException> {
                    service.execute(command)
                }

            exception.message shouldContain "닉네임 'existingNickname'"
            exception.message shouldContain "이미 사용 중입니다"

            // 이메일이 발송되지 않았는지 확인
            emailSender.getSentMailCount() shouldBe 0
            // 캐시에 저장되지 않았는지 확인
            cacheStore.size() shouldBe 0
        }

        test("유효한 데이터로 회원가입하면 PENDING 상태의 사용자를 생성한다") {
            // Given
            val fixedTime = Instant.parse("2025-01-05T10:00:00Z")
            clockHolder.setNow(fixedTime)
            idGenerator.setNextId("generated-user-id")

            val command =
                UserSignUpUseCase.Command(
                    email = Email("newuser@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("Password123!"),
                    nickname = Nickname("newuser"),
                )

            // When
            service.execute(command)

            // Then
            val savedUser = repository.findBy(Email("newuser@example.com"))
            savedUser.shouldNotBeNull()
            savedUser.id shouldBe "generated-user-id"
            savedUser.email shouldBe Email("newuser@example.com")
            savedUser.nickname shouldBe Nickname("newuser")
            savedUser.status shouldBe UserStatus.PENDING
            savedUser.createdAt shouldBe fixedTime
            savedUser.modifiedAt shouldBe fixedTime

            // 비밀번호가 암호화되었는지 확인
            val expectedEncodedPassword = passwordCipher.encode(RawPassword("Password123!"))
            savedUser.encodedPassword shouldBe expectedEncodedPassword
        }

        test("회원가입 성공 시 인증 메일을 발송한다") {
            // Given
            val command =
                UserSignUpUseCase.Command(
                    email = Email("newuser@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("Password123!"),
                    nickname = Nickname("newuser"),
                )

            // When
            service.execute(command)

            // Then
            emailSender.getSentMailCount() shouldBe 1

            val sentMail = emailSender.getLastSentMail()
            sentMail.shouldNotBeNull()
            sentMail.to shouldBe Email("newuser@example.com")
            sentMail.content.subject shouldBe "TechWiki+ 회원가입 인증 코드"
            sentMail.content.body shouldContain "이메일 주소를 인증해주세요"
            sentMail.content.body shouldContain "인증 코드"
        }

        test("인증 메일 발송 후 인증 코드를 캐시에 저장한다") {
            // Given
            val command =
                UserSignUpUseCase.Command(
                    email = Email("newuser@example.com"),
                    password = RawPassword("Password123!"),
                    confirmPassword = RawPassword("Password123!"),
                    nickname = Nickname("newuser"),
                )

            // When
            service.execute(command)

            // Then
            val expectedKey = "registration_code::Email(value=newuser@example.com)"
            cacheStore.hasKey(expectedKey) shouldBe true

            val cachedCode = cacheStore.get(expectedKey)
            cachedCode.shouldNotBeNull()
            cachedCode.length shouldBe 6
            cachedCode.all { it.isDigit() } shouldBe true

            // 이메일 본문에도 동일한 코드가 포함되어 있는지 확인
            val sentMail = emailSender.getLastSentMail()
            sentMail.shouldNotBeNull()
            sentMail.content.body shouldContain cachedCode
        }

        test("회원가입 전체 플로우가 올바른 순서로 실행된다") {
            // Given
            val fixedTime = Instant.parse("2025-01-05T12:00:00Z")
            clockHolder.setNow(fixedTime)
            idGenerator.setNextId("test-user-id")

            val command =
                UserSignUpUseCase.Command(
                    email = Email("fullflow@example.com"),
                    password = RawPassword("SecurePass123!"),
                    confirmPassword = RawPassword("SecurePass123!"),
                    nickname = Nickname("fullflowuser"),
                )

            // When
            service.execute(command)

            // Then
            // 1. 사용자가 PENDING 상태로 저장되었는지 확인
            val savedUser = repository.findBy(Email("fullflow@example.com"))
            savedUser.shouldNotBeNull()
            savedUser.status shouldBe UserStatus.PENDING

            // 2. 인증 메일이 발송되었는지 확인
            emailSender.getSentMailCount() shouldBe 1
            val sentMail = emailSender.getLastSentMail()
            sentMail.shouldNotBeNull()
            sentMail.to shouldBe Email("fullflow@example.com")

            // 3. 인증 코드가 캐시에 저장되었는지 확인
            val cacheKey = "registration_code::Email(value=fullflow@example.com)"
            val cachedCode = cacheStore.get(cacheKey)
            cachedCode.shouldNotBeNull()

            // 4. 이메일 본문에 캐시된 코드가 포함되어 있는지 확인
            sentMail.content.body shouldContain cachedCode
        }
    }
})
