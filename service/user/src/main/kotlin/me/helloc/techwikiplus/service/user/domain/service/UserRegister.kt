package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.PasswordsDoNotMatchException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.port.IdGenerator
import me.helloc.techwikiplus.service.user.domain.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserRegister(
    private val clockHolder: ClockHolder,
    private val idGenerator: IdGenerator,
    private val repository: UserRepository,
    private val passwordEncryptor: PasswordEncryptor,
) {
    @Throws(UserAlreadyExistsException::class, PasswordsDoNotMatchException::class)
    fun insert(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        passwordConfirm: RawPassword,
    ): User {
        if (password != passwordConfirm) {
            throw PasswordsDoNotMatchException()
        }

        if (repository.exists(email)) {
            throw UserAlreadyExistsException(email)
        }

        if (repository.exists(nickname)) {
            throw UserAlreadyExistsException(nickname)
        }

        val encodedPassword: EncodedPassword = passwordEncryptor.encode(rawPassword = password)
        val now: Instant = clockHolder.now()
        val user =
            User.create(
                id = UserId(idGenerator.next()),
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
