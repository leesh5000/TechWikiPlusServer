package me.helloc.techwikiplus.service.user.application.usecase

interface UserSignUpUseCase {
    /**
     * 사용자 회원가입을 처리하는 메서드
     *
     * @param email 사용자의 이메일 주소
     * @param password 사용자의 비밀번호
     * @param confirmPassword 비밀번호 확인용 문자열
     * @param nickname 사용자의 닉네임
     */
    fun signup(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
    )
}
