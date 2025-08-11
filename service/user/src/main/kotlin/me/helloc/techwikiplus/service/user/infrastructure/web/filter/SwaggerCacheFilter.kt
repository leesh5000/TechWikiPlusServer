package me.helloc.techwikiplus.service.user.infrastructure.web.filter

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

        // Swagger UI 및 OpenAPI 문서 관련 모든 경로에 대해 강력한 캐시 비활성화
        if (path.startsWith("/swagger-ui/") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/api-docs/") ||
            path == "/swagger-ui.html" ||
            path.contains("openapi") ||
            path.contains("swagger")
        ) {
            // 가장 강력한 캐시 비활성화 설정
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0")
            response.setHeader("Pragma", "no-cache")
            response.setHeader("Expires", "0")
            response.setHeader("Surrogate-Control", "no-store")
            
            // ETag 제거로 조건부 요청 방지
            response.setHeader("ETag", "")
            response.setHeader("Last-Modified", "")
        }

        filterChain.doFilter(request, response)
    }
}
