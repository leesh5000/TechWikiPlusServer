package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.IdGenerator
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Transactional
@Component
open class UserSignUpUseCase(
    private val userWriter: UserWriter,
    private val userDuplicateChecker: UserDuplicateChecker,
    private val userPasswordService: UserPasswordService,
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val idGenerator: IdGenerator,
) {
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        userDuplicateChecker.validateUserEmailDuplicate(email)
        userDuplicateChecker.validateUserNicknameDuplicate(nickname)
        val user =
            User.withPendingUser(
                id = idGenerator.next(),
                email = UserEmail(email, false),
                nickname = nickname,
                password = userPasswordService.validateAndEncode(password),
                clock = Clock.system,
            )
        userWriter.insertOrUpdate(user)
        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
        val ttl: Duration = Duration.ofMinutes(5)
        verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
    }
}
