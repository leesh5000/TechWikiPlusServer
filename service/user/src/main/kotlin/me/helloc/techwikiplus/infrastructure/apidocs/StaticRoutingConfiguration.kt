package me.helloc.techwikiplus.infrastructure.apidocs

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 정적 리소스 라우팅 설정
 *
 * REST Docs로 생성된 OpenAPI 문서를 제공하기 위한 설정입니다.
 */
@Configuration
class StaticRoutingConfiguration : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // REST Docs로 생성된 API 문서 제공
        registry
            .addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/static/api-docs/")
    }
}
