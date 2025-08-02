package me.helloc.techwikiplus.service.user.application

import me.helloc.techwikiplus.service.user.application.usecase.UserSignUpUseCase
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.PasswordConfirmationVerifier
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserSignUpFacade(
    private val writer: UserWriter,
    private val userPasswordService: UserPasswordService,
    private val passwordConfirmationVerifier: PasswordConfirmationVerifier,
    private val auditor: Auditor,
    private val userEmailVerificationCodeManager: UserEmailVerificationCodeManager,
    private val idGenerator: IdGenerator,
) : UserSignUpUseCase {
    override fun signup(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
    ) {
        val nicknameValue = Nickname(nickname)
        val userEmail = Email(email)
        passwordConfirmationVerifier.verify(password, confirmPassword)
        val encodedPassword: EncodedPassword = userPasswordService.encode(password)
        val user =
            User.create(
                id = idGenerator.next(),
                email = userEmail,
                encodedPassword = encodedPassword,
                nickname = nicknameValue,
                createdAt = auditor.generateCreateTime(),
                modifiedAt = auditor.generateCreateTime(),
            )
        val savedUser = writer.save(user)
        userEmailVerificationCodeManager.sendVerifyMailTo(savedUser)
    }
}
