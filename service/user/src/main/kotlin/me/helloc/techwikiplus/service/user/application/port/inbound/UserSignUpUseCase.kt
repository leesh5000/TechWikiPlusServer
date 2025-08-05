package me.helloc.techwikiplus.service.user.application.port.inbound

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

interface UserSignUpUseCase {
    /**
     * 사용자 회원가입을 처리하는 메서드
     *
     * @param command 회원가입에 필요한 정보를 담은 커맨드 객체
     * @throws UserAlreadyExistsException 이미 존재하는 사용자일 경우
     * @throws PasswordsDoNotMatchException 비밀번호가 일치하지 않을 경우
     */
    @Throws(UserAlreadyExistsException::class, PasswordsDoNotMatchException::class)
    fun execute(command: Command)

    data class Command(
        val email: Email,
        val password: RawPassword,
        val confirmPassword: RawPassword,
        val nickname: Nickname,
    )

    class UserAlreadyExistsException(
        message: String
    ) : RuntimeException(message) {
        constructor(email: Email) : this("이메일 '${email.value}'은(는) 이미 사용 중입니다.")
        constructor(nickname: Nickname) : this("닉네임 '${nickname.value}'은(는) 이미 사용 중입니다.")
    }

    class PasswordsDoNotMatchException() : RuntimeException(
        "비밀번호와 비밀번호 확인이 일치하지 않습니다."
    )
}
