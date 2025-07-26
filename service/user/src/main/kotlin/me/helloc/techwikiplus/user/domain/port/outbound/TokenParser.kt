package me.helloc.techwikiplus.user.domain.port.outbound

interface TokenParser {
    fun getEmailFromToken(token: String): String

    fun getUserIdFromToken(token: String): Long
}
