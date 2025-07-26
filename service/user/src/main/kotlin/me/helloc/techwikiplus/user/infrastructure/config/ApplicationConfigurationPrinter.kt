package me.helloc.techwikiplus.user.infrastructure.config

import me.helloc.techwikiplus.user.infrastructure.cors.CorsProperties
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ApplicationConfigurationPrinter(
    private val environment: Environment,
    private val jwtProperties: JwtProperties,
    private val corsProperties: CorsProperties,
    private val buildProperties: BuildProperties?,
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        printApplicationStartupInfo()
    }

    private fun printApplicationStartupInfo() {
        val banner =
            buildString {
                appendLine()
                appendLine("========================================")
                appendLine("   TechWiki Plus User Service")
                buildProperties?.let {
                    appendLine("   Version: ${it.version}")
                    appendLine("   Build Time: ${it.time}")
                } ?: appendLine("   Version: Unknown")
                appendLine("========================================")
                appendLine()

                appendLine("[Server Configuration]")
                appendLine("- Port: ${environment.getProperty("server.port", "8080")}")
                appendLine("- Application Name: ${environment.getProperty("spring.application.name", "Unknown")}")
                appendLine()

                appendLine("[Database Configuration]")
                val dbUrl = environment.getProperty("spring.datasource.url", "Not configured")
                val sanitizedDbUrl = dbUrl.substringBefore("?")
                appendLine("- URL: $sanitizedDbUrl")
                appendLine("- Username: ${environment.getProperty("spring.datasource.username", "Not configured")}")
                appendLine("- Password: ${maskPassword(environment.getProperty("spring.datasource.password", ""))}")
                appendLine(
                    "- Max Pool Size: ${environment.getProperty("spring.datasource.hikari.maximum-pool-size", "10")}",
                )
                appendLine("- Min Idle: ${environment.getProperty("spring.datasource.hikari.minimum-idle", "10")}")
                appendLine()

                appendLine("[Redis Configuration]")
                appendLine("- Host: ${environment.getProperty("spring.data.redis.host", "localhost")}")
                appendLine("- Port: ${environment.getProperty("spring.data.redis.port", "6379")}")
                appendLine("- Password: ${maskPassword(environment.getProperty("spring.data.redis.password", ""))}")
                appendLine()

                appendLine("[Security Configuration]")
                appendLine("- JWT Secret: ${maskPassword(jwtProperties.secret)}")
                appendLine("- JWT Access Token Expiration: ${formatDuration(jwtProperties.accessTokenExpiration)}")
                appendLine("- JWT Refresh Token Expiration: ${formatDuration(jwtProperties.refreshTokenExpiration)}")
                appendLine()

                appendLine("[Mail Configuration]")
                appendLine("- Host: ${environment.getProperty("spring.mail.host", "Not configured")}")
                appendLine("- Port: ${environment.getProperty("spring.mail.port", "587")}")
                appendLine("- Username: ${maskEmail(environment.getProperty("spring.mail.username", ""))}")
                appendLine("- Password: ${maskPassword(environment.getProperty("spring.mail.password", ""))}")
                appendLine("- SMTP Auth: ${environment.getProperty("spring.mail.properties.mail.smtp.auth", "false")}")
                appendLine(
                    "- STARTTLS Enable: ${environment.getProperty(
                        "spring.mail.properties.mail.smtp.starttls.enable",
                        "false",
                    )}",
                )
                appendLine(
                    "- STARTTLS Required: ${environment.getProperty(
                        "spring.mail.properties.mail.smtp.starttls.required",
                        "false",
                    )}",
                )
                appendLine()

                appendLine("[CORS Configuration]")
                appendLine("- Allowed Origins: ${corsProperties.allowedOrigins}")
                appendLine("- Allowed Methods: ${corsProperties.allowedMethods}")
                appendLine("- Allow Credentials: ${corsProperties.allowCredentials}")
                appendLine()

                appendLine("[Logging Configuration]")
                appendLine("- Root Level: ${environment.getProperty("logging.level.root", "INFO")}")
                appendLine(
                    "- Application Level: ${environment.getProperty("logging.level.me.helloc.techwikiplus", "INFO")}",
                )
                appendLine()

                appendLine("[Environment]")
                appendLine("- Active Profiles: ${environment.activeProfiles.joinToString(", ").ifEmpty { "default" }}")
                appendLine("- Java Version: ${System.getProperty("java.version")}")
                appendLine("- Java Vendor: ${System.getProperty("java.vendor")}")
                appendLine("========================================")
            }

        logger.info(banner)
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    private fun maskEmail(email: String): String {
        if (email.isEmpty() || !email.contains("@")) return "****"

        val parts = email.split("@")
        val username = parts[0]
        val domain = parts[1]

        val maskedUsername =
            if (username.length <= 3) {
                "*".repeat(username.length)
            } else {
                username.take(3) + "*".repeat(username.length - 3)
            }

        return "$maskedUsername@$domain"
    }

    private fun maskPassword(password: String): String {
        if (password.isEmpty()) return "****"

        return when {
            password.length <= 4 -> "*".repeat(password.length)
            else -> {
                val firstTwo = password.take(2)
                val lastOne = password.takeLast(1)
                val middleLength = password.length - 3
                "$firstTwo${"*".repeat(middleLength)}$lastOne"
            }
        }
    }
}
