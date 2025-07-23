package me.helloc.techwikiplus.user.domain.service

interface TokenParser {
    fun getEmailFromToken(token: String): String

    fun getUserIdFromToken(token: String): Long
}
