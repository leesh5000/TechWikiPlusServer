package me.helloc.techwikiplus.service.user.interfaces.dto

data class HealthCheckResponse(
    val status: String,
    val version: String,
    val serviceName: String,
)
