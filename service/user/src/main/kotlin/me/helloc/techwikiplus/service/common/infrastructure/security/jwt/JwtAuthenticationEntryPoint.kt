package me.helloc.techwikiplus.service.common.infrastructure.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.common.interfaces.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)
        private const val DEFAULT_ERROR_MESSAGE = "인증이 필요합니다"
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        logger.debug("Authentication failed: ${authException.message}")

        val errorResponse =
            ErrorResponse.of(
                code = "UNAUTHORIZED",
                message = DEFAULT_ERROR_MESSAGE,
            )

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
