package me.helloc.techwikiplus.service.user.infrastructure.web.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC 설정
 *
 * 정적 리소스에 대한 캐싱 전략과 버전 관리를 설정합니다.
 * Swagger UI와 API 문서는 캐시를 비활성화하여 항상 최신 버전을 제공합니다.
 */
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // API 문서에 대한 캐싱 전략 설정
        registry.addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/static/api-docs/")
            .setCacheControl(
                // 모든 환경에서 캐시 비활성화 - 항상 최신 API 문서를 보장
                CacheControl.noStore(),
            )

        // Swagger UI 정적 리소스 캐싱 설정
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
            .setCacheControl(
                // 모든 환경에서 캐시 비활성화 - 항상 최신 UI를 보장
                CacheControl.noStore(),
            )
    }
}
