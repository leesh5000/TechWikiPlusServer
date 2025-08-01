package me.helloc.techwikiplus.interfaces.dto

data class HealthCheckResponse(
    val status: String,
    val version: String,
    val serviceName: String,
    val timestamp: Long? = null,
)
