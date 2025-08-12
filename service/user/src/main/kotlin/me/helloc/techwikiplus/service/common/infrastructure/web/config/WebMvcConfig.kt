package me.helloc.techwikiplus.service.common.infrastructure.web.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC 설정
 *
 * API 문서에 대한 캐싱 전략을 설정합니다.
 * Swagger UI는 SpringDoc이 자동으로 처리하므로 별도 설정하지 않습니다.
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
    }
}
