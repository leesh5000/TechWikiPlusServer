package me.helloc.techwikiplus.user.interfaces.http

data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val timestamp: String,
    val path: String,
)
