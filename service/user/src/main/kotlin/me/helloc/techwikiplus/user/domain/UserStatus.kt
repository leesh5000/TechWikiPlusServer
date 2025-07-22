package me.helloc.techwikiplus.user.domain

enum class UserStatus {
    ACTIVE, // 활성 상태
    PENDING, // 회원가입 대기 중 상태
    BANNED, // 계정 정지
    DORMANT, // 휴면 계정 (6개월 이상 로그인하지 않은 상태)
    DELETED, // 계정 삭제됨
}
