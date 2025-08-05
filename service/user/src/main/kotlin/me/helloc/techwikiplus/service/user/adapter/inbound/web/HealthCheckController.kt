package me.helloc.techwikiplus.service.user.adapter.inbound.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController(
    @Value("\${spring.application.version}")
    private val version: String,
    @Value("\${spring.application.name}")
    private val serviceName: String,
) {
    @GetMapping("/health")
    fun healthCheck(): Response {
        return Response(
            status = "UP",
            version = version,
            serviceName = serviceName,
        )
    }

    data class Response(
        val status: String,
        val version: String,
        val serviceName: String,
    )
}
