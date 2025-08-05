package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.adapter.outbound.cache.VerificationCodeFakeStore
import me.helloc.techwikiplus.service.user.adapter.outbound.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.adapter.outbound.id.FakeIdGenerator
import me.helloc.techwikiplus.service.user.adapter.outbound.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.application.port.inbound.UserVerifyUseCase
import me.helloc.techwikiplus.service.user.application.service.UserVerifyFacade
import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import java.time.Instant

class UserVerifyFacadeIntegrationTest : FunSpec({

    test("올바른 이메일과 인증 코드 번호로 PENDING 상태의 사용자 인증을 완료해야 한다") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val verificationCodeStore = VerificationCodeFakeStore()
        val clockHolder = FakeClockHolder(now = now)
        val auditor = Auditor(clockHolder)

        val userReader = UserReader(repository)
        val userWriter = UserWriter(repository)
        val idGenerator = FakeIdGenerator()

        val sut =
            UserVerifyFacade(
                userReader = userReader,
                userWriter = userWriter,
                verificationCodeStore = verificationCodeStore,
                auditor = auditor,
            )

        // PENDING 상태의 사용자 생성 및 저장
        val email = Email("test@example.com")
        val user =
            User(
                id = idGenerator.next(),
                email = email,
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded-password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(user)

        // 인증 코드를 캐시에 저장
        val registrationCode = RegistrationCode("123456")
        verificationCodeStore.store(email, registrationCode)

        // When
        sut.execute(UserVerifyUseCase.Command(email, registrationCode))

        // Then
        val updatedUser = repository.findBy(email)
        updatedUser?.status shouldBe UserStatus.ACTIVE
        updatedUser?.modifiedAt shouldBe now
    }

    test("잘못된 인증 코드로 검증 실패 시 InvalidVerificationCodeException 발생") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val verificationCodeStore = VerificationCodeFakeStore()
        val clockHolder = FakeClockHolder(now = now)
        val auditor = Auditor(clockHolder)

        val userReader = UserReader(repository)
        val userWriter = UserWriter(repository)
        val idGenerator = FakeIdGenerator()

        val sut =
            UserVerifyFacade(
                userReader = userReader,
                userWriter = userWriter,
                verificationCodeStore = verificationCodeStore,
                auditor = auditor,
            )

        // PENDING 상태의 사용자 생성 및 저장
        val email = Email("test@example.com")
        val user =
            User(
                id = idGenerator.next(),
                email = email,
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded-password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(user)

        // 인증 코듌를 캐시에 저장
        val correctCode = RegistrationCode("123456")
        val wrongCode = RegistrationCode("999999")
        verificationCodeStore.store(email, correctCode)

        // When & Then
        shouldThrow<InvalidVerificationCodeException> {
            sut.execute(UserVerifyUseCase.Command(email, wrongCode))
        }

        // 사용자 상태가 변경되지 않았는지 확인
        val unchangedUser = repository.findBy(email)
        unchangedUser?.status shouldBe UserStatus.PENDING
    }

    test("존재하지 않는 이메일로 검증 시도 시 UserNotFoundException 발생") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val verificationCodeStore = VerificationCodeFakeStore()
        val clockHolder = FakeClockHolder(now = now)
        val auditor = Auditor(clockHolder)

        val userReader = UserReader(repository)
        val userWriter = UserWriter(repository)

        val sut =
            UserVerifyFacade(
                userReader = userReader,
                userWriter = userWriter,
                verificationCodeStore = verificationCodeStore,
                auditor = auditor,
            )

        val nonExistentEmail = Email("nonexistent@example.com")
        val registrationCode = RegistrationCode("123456")

        // When & Then
        shouldThrow<UserNotFoundException> {
            sut.execute(UserVerifyUseCase.Command(nonExistentEmail, registrationCode))
        }
    }

    test("인증 코드가 캐시에 없는 경우 InvalidVerificationCodeException 발생") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val verificationCodeStore = VerificationCodeFakeStore()
        val clockHolder = FakeClockHolder(now = now)
        val auditor = Auditor(clockHolder)

        val userReader = UserReader(repository)
        val userWriter = UserWriter(repository)
        val idGenerator = FakeIdGenerator()

        val sut =
            UserVerifyFacade(
                userReader = userReader,
                userWriter = userWriter,
                verificationCodeStore = verificationCodeStore,
                auditor = auditor,
            )

        // PENDING 상태의 사용자 생성 및 저장
        val email = Email("test@example.com")
        val user =
            User(
                id = idGenerator.next(),
                email = email,
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded-password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(user)

        // 캐시에 인증 코드를 저장하지 않음
        val registrationCode = RegistrationCode("123456")

        // When & Then
        shouldThrow<InvalidVerificationCodeException> {
            sut.execute(UserVerifyUseCase.Command(email, registrationCode))
        }

        // 사용자 상태가 변경되지 않았는지 확인
        val unchangedUser = repository.findBy(email)
        unchangedUser?.status shouldBe UserStatus.PENDING
    }
})
