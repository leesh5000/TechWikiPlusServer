package me.helloc.techwikiplus.user.infrastructure.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.mail")
data class MailProperties(
    val type: String = "smtp",
    val host: String = "localhost",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val properties: Map<String, Any> = emptyMap(),
)
