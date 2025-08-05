package me.helloc.techwikiplus.service.user.application

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.PasswordConfirmationVerifier
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordEncoder
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserSignUpUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserSignUpFacade(
    private val writer: UserWriter,
    private val userPasswordEncoder: UserPasswordEncoder,
    private val passwordConfirmationVerifier: PasswordConfirmationVerifier,
    private val auditor: Auditor,
    private val userEmailVerificationCodeManager: UserEmailVerificationCodeManager,
    private val idGenerator: IdGenerator,
) : UserSignUpUseCase {
    override fun execute(command: UserSignUpUseCase.Command) {
        val nicknameValue = Nickname(command.nickname)
        val userEmail = Email(command.email)
        val rawPassword = RawPassword(command.password)
        val rawConfirmPassword = RawPassword(command.confirmPassword)
        passwordConfirmationVerifier.equalsOrThrows(rawPassword, rawConfirmPassword)
        val encodedPassword: EncodedPassword = userPasswordEncoder.encode(rawPassword)
        val user =
            User.create(
                id = idGenerator.next(),
                email = userEmail,
                encodedPassword = encodedPassword,
                nickname = nicknameValue,
                createdAt = auditor.generateCreateTime(),
                modifiedAt = auditor.generateCreateTime(),
            )
        val savedUser = writer.insert(user)
        userEmailVerificationCodeManager.sendVerifyMailTo(savedUser)
    }
}
