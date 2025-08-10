package me.helloc.techwikiplus.service.user.infrastructure.web.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.VersionResourceResolver

/**
 * Web MVC 설정
 *
 * 정적 리소스에 대한 캐싱 전략과 버전 관리를 설정합니다.
 * 애플리케이션 버전을 기반으로 캐시를 무효화하여
 * 배포 시 자동으로 최신 문서가 로드되도록 합니다.
 */
@Configuration
class WebMvcConfig(
    @param:Value("\${spring.application.version:LOCAL_VERSION}")
    private val appVersion: String,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 로컬 환경에서는 캐시 비활성화, 배포 환경에서는 버전 기반 캐싱 (로컬: 0, 배포: 1년)
        val cachePeriod = if (appVersion == "LOCAL_VERSION") 0 else 31536000

        // API 문서에 대한 캐싱 전략 설정
        registry.addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/static/api-docs/")
            .setCachePeriod(cachePeriod)
            .resourceChain(true)
            .addResolver(
                VersionResourceResolver()
                    // 앱 버전 기반 버전 관리 - URL에 버전 추가
                    .addFixedVersionStrategy(appVersion, "/**"),
            )

        // Swagger UI 정적 리소스 캐싱 설정
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
            .setCachePeriod(cachePeriod)
            .resourceChain(true)
            .addResolver(
                VersionResourceResolver()
                    // 앱 버전 기반 버전 관리 - URL에 버전 추가
                    .addFixedVersionStrategy(appVersion, "/**"),
            )
    }
}
