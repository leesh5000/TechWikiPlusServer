package me.helloc.techwikiplus.service.user.interfaces.usecase

interface UserSignUpUseCase {
    /**
     * 사용자 회원가입을 처리하는 메서드
     *
     * @param command 회원가입에 필요한 정보를 담은 커맨드 객체
     */
    fun execute(command: Command)

    data class Command(
        val email: String,
        val password: String,
        val confirmPassword: String,
        val nickname: String,
    )
}
