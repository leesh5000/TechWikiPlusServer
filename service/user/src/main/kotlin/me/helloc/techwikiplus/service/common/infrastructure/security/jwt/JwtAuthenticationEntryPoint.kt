package me.helloc.techwikiplus.service.common.infrastructure.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.common.interfaces.web.ErrorResponse
import me.helloc.techwikiplus.service.user.interfaces.web.UserErrorCodeMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val userErrorCodeMapper: UserErrorCodeMapper,
) : AuthenticationEntryPoint {
    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)
        private const val DEFAULT_ERROR_MESSAGE = "인증이 필요합니다"
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?,
    ) {
        logger.debug("Authentication failed: ${authException?.message}")

        val (httpStatus, errorCode, errorMessage) =
            when (authException) {
                is UserStatusAuthenticationException -> {
                    val status = userErrorCodeMapper.mapToHttpStatus(authException.errorCode)
                    val message = userErrorCodeMapper.mapToMessage(authException.errorCode, emptyArray())
                    Triple(status, authException.errorCode.name, message)
                }
                else -> {
                    Triple(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        authException?.message ?: DEFAULT_ERROR_MESSAGE,
                    )
                }
            }

        val errorResponse =
            ErrorResponse.of(
                code = errorCode,
                message = errorMessage,
            )

        response.status = httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
