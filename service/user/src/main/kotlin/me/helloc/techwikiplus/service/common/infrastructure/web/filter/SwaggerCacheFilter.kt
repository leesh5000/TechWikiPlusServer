package me.helloc.techwikiplus.service.common.infrastructure.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Swagger UI 캐시 비활성화 필터
 *
 * SpringDoc이 자동으로 등록하는 Swagger UI 리소스에 대해
 * 캐시를 비활성화하여 항상 최신 API 문서를 표시합니다.
 */
@Component
class SwaggerCacheFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI

        // Swagger UI 관련 경로에 대해 캐시 비활성화
        if (path.startsWith("/swagger-ui/") ||
            path.startsWith("/v3/api-docs") ||
            path == "/swagger-ui.html"
        ) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
            response.setHeader("Pragma", "no-cache")
            response.setHeader("Expires", "0")
        }

        filterChain.doFilter(request, response)
    }
}
