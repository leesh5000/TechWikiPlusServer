package me.helloc.techwikiplus.user.domain

import me.helloc.techwikiplus.user.domain.exception.CustomException

enum class UserStatus {
    ACTIVE, // 활성 상태
    PENDING, // 회원가입 대기 중 상태
    BANNED, // 계정 정지
    DORMANT, // 휴면 계정 (6개월 이상 로그인하지 않은 상태)
    DELETED, // 계정 삭제됨
    ;

    fun validateForAuthentication() {
        when (this) {
            ACTIVE -> return
            PENDING -> throw CustomException.AuthenticationException.EmailNotVerified()
            BANNED -> throw CustomException.AuthenticationException.AccountBanned()
            DORMANT -> throw CustomException.AuthenticationException.AccountDormant()
            DELETED -> throw CustomException.AuthenticationException.AccountDeleted()
        }
    }
}
