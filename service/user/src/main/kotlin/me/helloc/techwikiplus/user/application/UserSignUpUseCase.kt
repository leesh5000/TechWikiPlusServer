package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.IdGenerator
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.SignUpPendingUserService
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Transactional
@Component
open class UserSignUpFacade(
    private val userWriter: UserWriter,
    private val userDuplicateChecker: UserDuplicateChecker,
    private val userPasswordService: UserPasswordService,
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val signUpPendingUserService: SignUpPendingUserService,
) {

    fun signUp(email: String, nickname: String, password: String, ) {
        userDuplicateChecker.validateUserEmailDuplicate(email)
        userDuplicateChecker.validateUserNicknameDuplicate(nickname)
        val user = User(
            id = idGenerator.next(),
            nickname = nickname,
            email = email,
            password = userPasswordService.validateAndEncode(password),
            status = UserStatus.PENDING,
            clock = clock)
        userWriter.insertOrUpdate(user)
        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
        val ttl: Duration = Duration.ofMinutes(5)
        verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
    }

    fun resendVerificationCode(email: String) {
        // 현재 "PENDING" 상태의 사용자만 재전송 가능
        signUpPendingUserService.existsOrThrows(email)
        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
        val ttl: Duration = Duration.ofMinutes(5)
        verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
    }
}
