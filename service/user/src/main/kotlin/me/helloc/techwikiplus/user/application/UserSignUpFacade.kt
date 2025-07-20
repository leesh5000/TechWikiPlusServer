package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class UserSignUpFacade(
    private val userDuplicateChecker: UserDuplicateChecker,
    private val userPasswordService: UserPasswordService,
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore
) {
    /**
     * 회원가입을 위한 메서드
     * @param email 사용자 이메일
     * @param nickname 사용자 닉네임
     * @param password 사용자 비밀번호
     * @throws me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidPassword 비밀번호가 유효하지 않은 경우
     * @throws me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateEmail 이메일 중복인 경우
     * @throws me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateNickname 닉네임 중복인 경우
     */
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        userPasswordService.validate(password)
        userDuplicateChecker.validateUserEmailDuplicate(email)
        userDuplicateChecker.validateUserNicknameDuplicate(nickname)

        val user = User(
        )

        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
        verificationCodeStore.storeUserWithExpiry(
            email,
            verificationCode,
            Duration.ofMinutes(5)
        )
    }

    fun verifyEmail(
        email: String,
        code: String
    ) {
        val verificationCode = VerificationCode(code)
        verificationCodeStore.retrieveUserOrThrows(email, verificationCode)

    }

}
