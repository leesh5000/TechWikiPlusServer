package me.helloc.techwikiplus.application.usecase

interface UserSignUpUseCase {
    /**
     * 사용자 회원가입을 처리하는 메서드
     *
     * @param email 사용자의 이메일 주소
     * @param password 사용자의 비밀번호
     * @param name 사용자의 이름
     * @return 회원가입 성공 여부
     */
    fun signup(email: String, password: String, name: String): Boolean
}
