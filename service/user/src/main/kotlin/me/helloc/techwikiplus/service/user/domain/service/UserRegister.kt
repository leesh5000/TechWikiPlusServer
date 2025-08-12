package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserRegister(
    private val clockHolder: ClockHolder,
    private val idGenerator: IdGenerator,
    private val repository: UserRepository,
    private val passwordEncryptor: PasswordEncryptor,
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

        if (repository.exists(email)) {
            throw UserDomainException(UserErrorCode.DUPLICATE_EMAIL, arrayOf(email.value))
        }

        if (repository.exists(nickname)) {
            throw UserDomainException(UserErrorCode.DUPLICATE_NICKNAME, arrayOf(nickname.value))
        }

        val encodedPassword: EncodedPassword = passwordEncryptor.encode(rawPassword = password)
        val now: Instant = clockHolder.now()
        val user =
            User.create(
                id = idGenerator.next(),
                email = email,
                encodedPassword = encodedPassword,
                nickname = nickname,
                status = UserStatus.ACTIVE,
                createdAt = now,
                modifiedAt = now,
            )
        return repository.save(user)
    }

    fun update(user: User): User {
        return repository.save(user)
    }
}
