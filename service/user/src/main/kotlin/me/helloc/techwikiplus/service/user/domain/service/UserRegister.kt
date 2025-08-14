package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.LockManager
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.service.port.UserIdGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration.ofSeconds
import java.time.Instant

@Service
class UserRegister(
    private val clockHolder: ClockHolder,
    private val userIdGenerator: UserIdGenerator,
    private val repository: UserRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val lockManager: LockManager,
) {
    @Throws(UserDomainException::class)
    fun insert(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        passwordConfirm: RawPassword,
    ): User {
        if (password != passwordConfirm) {
            throw UserDomainException(UserErrorCode.PASSWORD_MISMATCH)
        }

        // 분산 락을 사용하여 이메일 기준 동시성 제어
        val lockKey = "user:register:${email.value}"

        return lockManager.executeWithLock(
            key = lockKey,
            // 5초 대기
            waitTime = ofSeconds(5),
            // 15초 후 자동 해제
            leaseTime = ofSeconds(15),
        ) {
            // 락을 획득한 상태에서 중복 검사 및 사용자 생성 수행
            performUserRegistration(email, nickname, password)
        }
    }

    /**
     * 분산 락 내에서 실제 사용자 등록 로직을 수행합니다.
     */
    private fun performUserRegistration(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
    ): User {
        // 중복 검사
        if (repository.exists(email)) {
            throw UserDomainException(UserErrorCode.DUPLICATE_EMAIL, arrayOf(email.value))
        }

        if (repository.exists(nickname)) {
            throw UserDomainException(UserErrorCode.DUPLICATE_NICKNAME, arrayOf(nickname.value))
        }

        // 사용자 생성 및 저장
        val encodedPassword: EncodedPassword = passwordEncryptor.encode(rawPassword = password)
        val now: Instant = clockHolder.now()
        val user =
            User.create(
                id = userIdGenerator.next(),
                email = email,
                encodedPassword = encodedPassword,
                nickname = nickname,
                createdAt = now,
                modifiedAt = now,
            )

        return repository.save(user)
    }

    fun update(user: User): User {
        return repository.save(user)
    }
}
