package me.helloc.techwikiplus.user.documentation.config

import org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer
import org.springframework.restdocs.operation.preprocess.Preprocessors

/**
 * REST Docs 설정을 위한 테스트 구성 클래스
 */
@TestConfiguration
class RestDocsConfiguration {
    @Bean
    fun restDocsMockMvcConfigurationCustomizer(): RestDocsMockMvcConfigurationCustomizer {
        return RestDocsMockMvcConfigurationCustomizer { configurer: MockMvcRestDocumentationConfigurer ->
            configurer
                .operationPreprocessors()
                .withRequestDefaults(
                    Preprocessors.prettyPrint(),
                    Preprocessors.removeHeaders("X-CSRF-TOKEN", "Host"),
                )
                .withResponseDefaults(
                    Preprocessors.prettyPrint(),
                    Preprocessors.removeHeaders(
                        "X-Content-Type-Options",
                        "X-XSS-Protection",
                        "Cache-Control",
                        "Pragma",
                        "Expires",
                        "X-Frame-Options",
                    ),
                )
        }
    }
}
