package me.helloc.techwikiplus.user.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("*"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val allowCredentials: Boolean = true,
    val maxAge: Long = 3600,
)

@Configuration
open class CorsConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins = corsProperties.allowedOrigins
                allowedMethods = corsProperties.allowedMethods
                allowedHeaders = corsProperties.allowedHeaders
                allowCredentials = corsProperties.allowCredentials
                maxAge = corsProperties.maxAge
            }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
