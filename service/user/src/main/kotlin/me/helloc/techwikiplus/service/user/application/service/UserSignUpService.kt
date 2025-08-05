package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.application.port.inbound.UserSignUpUseCase
import me.helloc.techwikiplus.service.user.application.port.outbound.CacheStore
import me.helloc.techwikiplus.service.user.application.port.outbound.ClockHolder
import me.helloc.techwikiplus.service.user.application.port.outbound.EmailSender
import me.helloc.techwikiplus.service.user.application.port.outbound.IdGenerator
import me.helloc.techwikiplus.service.user.application.port.outbound.PasswordCipher
import me.helloc.techwikiplus.service.user.application.port.outbound.UserRepository
import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.RegistrationMailTemplate
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@Service
class UserSignUpService(
    private val clockHolder: ClockHolder,
    private val idGenerator: IdGenerator,
    private val repository: UserRepository,
    private val passwordCipher: PasswordCipher,
    private val emailSender: EmailSender,
    private val cacheStore: CacheStore
) : UserSignUpUseCase {

    override fun execute(command: UserSignUpUseCase.Command) {
        val user = insertPendingUser(command)
        val registrationCode: RegistrationCode = sendRegistrationMail(user)
        storeRegistrationCode(registrationCode, user.email)
    }

    private fun storeRegistrationCode(
        registrationCode: RegistrationCode,
        email: Email
    ) {
        val registrationCodeKey = "service::user::registration_code::$email"
        cacheStore.put(registrationCodeKey, registrationCode.value)
    }

    private fun sendRegistrationMail(user: User): RegistrationCode {
        val registrationCode: RegistrationCode = RegistrationCode.generate()
        val mail: MailContent = RegistrationMailTemplate.of(registrationCode)
        emailSender.send(user.email, mail)
        return registrationCode
    }

    private fun insertPendingUser(command: UserSignUpUseCase.Command): User {

        if (command.password != command.confirmPassword) {
            throw UserSignUpUseCase.PasswordsDoNotMatchException()
        }

        if (repository.exists(command.email)) {
            throw UserSignUpUseCase.UserAlreadyExistsException(command.email)
        }

        if (repository.exists(command.nickname)) {
            throw UserSignUpUseCase.UserAlreadyExistsException(command.nickname)
        }

        val encodedPassword: EncodedPassword = passwordCipher.encode(rawPassword = command.password)
        val now: Instant = clockHolder.now()
        val user =
            User.create(
                id = idGenerator.next(),
                email = command.email,
                encodedPassword = encodedPassword,
                nickname = command.nickname,
                status = UserStatus.PENDING,
                createdAt = now,
                modifiedAt = now,
            )
        repository.insert(user)
        return user
    }
}
