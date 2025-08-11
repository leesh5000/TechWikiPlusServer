package me.helloc.techwikiplus.service.user.infrastructure.web.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.VersionResourceResolver
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Web MVC 설정
 *
 * 정적 리소스에 대한 캐싱 전략과 버전 관리를 설정합니다.
 * 배포마다 고유한 UUID를 생성하여 캐시를 무효화하고,
 * 배포 시 자동으로 최신 문서가 로드되도록 합니다.
 */
@Configuration
class WebMvcConfig(
    @param:Value("\${spring.application.version:LOCAL_VERSION}")
    private val appVersion: String,
    @param:Value("\${IMAGE_TAG:#{null}}")
    private val imageTag: String?,
) : WebMvcConfigurer {
    // 배포마다 고유한 버전 식별자 생성
    // 우선순위: IMAGE_TAG > appVersion > UUID
    private val deploymentVersion: String =
        when {
            !imageTag.isNullOrBlank() -> imageTag
            appVersion != "LOCAL_VERSION" -> appVersion
            else -> UUID.randomUUID().toString()
        }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val isLocalEnvironment = appVersion == "LOCAL_VERSION" && imageTag.isNullOrBlank()

        // API 문서에 대한 캐싱 전략 설정
        registry.addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/static/api-docs/")
            .setCacheControl(
                if (isLocalEnvironment) {
                    // 로컬 환경: 캐시 비활성화
                    CacheControl.noCache()
                } else {
                    // 프로덕션 환경: 1년 캐싱 + must-revalidate
                    CacheControl.maxAge(365, TimeUnit.DAYS)
                        .mustRevalidate()
                        .cachePublic()
                },
            )
            .resourceChain(true)
            .addResolver(
                VersionResourceResolver()
                    // 배포 버전 기반 캐시 버스팅
                    .addFixedVersionStrategy(deploymentVersion, "/**"),
            )

        // Swagger UI 정적 리소스 캐싱 설정
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
            .setCacheControl(
                if (isLocalEnvironment) {
                    // 로컬 환경: 캐시 비활성화
                    CacheControl.noCache()
                } else {
                    // 프로덕션 환경: 1년 캐싱 + must-revalidate
                    CacheControl.maxAge(365, TimeUnit.DAYS)
                        .mustRevalidate()
                        .cachePublic()
                },
            )
            .resourceChain(true)
            .addResolver(
                VersionResourceResolver()
                    // 배포 버전 기반 캐시 버스팅
                    .addFixedVersionStrategy(deploymentVersion, "/**"),
            )
    }
}
