package me.helloc.techwikiplus.user.infrastructure.documentation

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Swagger UI 설정
 *
 * REST Docs와 restdocs-api-spec으로 생성된 OpenAPI 스펙을
 * Swagger UI로 표시하기 위한 설정
 */
@Configuration
class SwaggerUIConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Swagger UI 정적 리소스 핸들러 추가
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.10.3/")

        // API 문서 정적 리소스 핸들러 추가
        registry.addResourceHandler("/docs/**")
            .addResourceLocations("classpath:/static/docs/")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // /swagger-ui.html로 접근 시 정적 HTML 파일로 리다이렉트
        registry.addRedirectViewController("/swagger-ui.html", "/docs/swagger-ui.html")
        registry.addRedirectViewController("/", "/swagger-ui.html")
    }
}
