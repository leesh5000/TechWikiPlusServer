package me.helloc.techwikiplus.user.interfaces.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@Configuration
class MessageSourceConfig {
    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename("classpath:messages")
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setCacheSeconds(3600) // 1시간 캐시
        messageSource.setFallbackToSystemLocale(false) // 시스템 로케일 대신 기본 messages.properties 사용
        return messageSource
    }
}
