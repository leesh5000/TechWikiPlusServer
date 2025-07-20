package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import org.springframework.stereotype.Component

@Component
class UserSignUpFacade(
    private val userDuplicateChecker: UserDuplicateChecker,
    private val userPasswordService: UserPasswordService,
    private val mailSender: MailSender,
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
        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
    }

}
