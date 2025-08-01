package me.helloc.techwikiplus.interfaces

import me.helloc.techwikiplus.interfaces.dto.HealthCheckResponse
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
    fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse(
            status = "UP",
            version = version,
            serviceName = serviceName,
            timestamp = System.currentTimeMillis(),
        )
    }
}
