package me.helloc.techwikiplus.service.common.infrastructure.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {
    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = extractTokenFromHeader(request)

            if (token != null) {
                val userId = jwtTokenManager.validateAccessToken(token)

                // Fetch user to get role information
                val user = userRepository.findBy(userId)

                if (user != null) {
                    // Check user status - may throw UserDomainException
                    try {
                        user.validateUserStatus()
                    } catch (e: UserDomainException) {
                        // Convert to AuthenticationException and call EntryPoint
                        val authException =
                            UserStatusAuthenticationException(
                                e.userErrorCode,
                                e.message ?: "Authentication failed due to user status",
                            )
                        log.debug("User status validation failed: ${e.userErrorCode}")
                        authenticationEntryPoint.commence(request, response, authException)
                        return // Stop filter chain
                    }

                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities,
                        )

                    SecurityContextHolder.getContext().authentication = authentication
                    log.debug("JWT authentication successful for user: ${userId.value} with role: ${user.role.name}")
                } else {
                    // User not found - treat as invalid token
                    val authException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_NOT_FOUND,
                            "User not found",
                        )
                    log.debug("User not found for userId: ${userId.value}")
                    authenticationEntryPoint.commence(request, response, authException)
                    return
                }
            }
        } catch (e: UserDomainException) {
            // Handle token validation errors
            val authException =
                UserStatusAuthenticationException(
                    e.userErrorCode,
                    e.message ?: "Token validation failed",
                )
            log.debug("Token validation failed: ${e.userErrorCode}")
            authenticationEntryPoint.commence(request, response, authException)
            return
        } catch (e: Exception) {
            // Other unexpected exceptions - just log and continue
            log.debug("JWT authentication failed with unexpected error: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)

        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
