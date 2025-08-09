package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.user.domain.port.TokenManager
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenManager: TokenManager,
    private val userRepository: UserRepository,
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
                val authorities = if (user != null) {
                    listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                } else {
                    emptyList()
                }

                val authentication =
                    UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities,
                    )

                SecurityContextHolder.getContext().authentication = authentication
                log.debug("JWT authentication successful for user: ${userId.value} with role: ${user?.role?.name}")
            }
        } catch (e: Exception) {
            log.debug("JWT authentication failed: ${e.message}")
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
