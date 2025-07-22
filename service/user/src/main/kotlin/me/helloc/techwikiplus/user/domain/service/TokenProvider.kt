package me.helloc.techwikiplus.user.domain.service

interface TokenProvider {
    fun createAccessToken(
        email: String,
        userId: Long,
    ): String

    fun createRefreshToken(
        email: String,
        userId: Long,
    ): String

    fun validateToken(token: String): Boolean

    fun getEmailFromToken(token: String): String

    fun getUserIdFromToken(token: String): Long

    fun getTokenType(token: String): String

    // 테스트 목적으로 만료된 refresh token 생성
    fun createExpiredRefreshToken(
        email: String,
        userId: Long,
    ): String {
        throw UnsupportedOperationException("This method is for testing purposes only")
    }
}
