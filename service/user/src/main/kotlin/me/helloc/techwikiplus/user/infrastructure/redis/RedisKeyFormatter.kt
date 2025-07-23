package me.helloc.techwikiplus.user.infrastructure.redis

/**
 * Redis 키 포맷을 중앙에서 관리하는 유틸리티 클래스
 */
object RedisKeyFormatter {
    // Refresh Token 관련 키
    fun refreshTokenKey(token: String): String = "refresh_token:$token"

    fun userRefreshTokenKey(userId: Long): String = "user_refresh_token:$userId"

    // Verification Code 관련 키
    fun verificationCodeKey(email: String): String = "user:verification:email:$email"

    fun verificationAttemptKey(email: String): String = "user:verification:attempt:email:$email"
}
