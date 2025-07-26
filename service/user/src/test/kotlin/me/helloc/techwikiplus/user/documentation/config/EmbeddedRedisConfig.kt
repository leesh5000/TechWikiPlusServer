package me.helloc.techwikiplus.user.documentation.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import redis.embedded.RedisServer

@TestConfiguration
@Profile("documentation")
class EmbeddedRedisConfig {
    private var redisServer: RedisServer? = null

    @PostConstruct
    fun startRedis() {
        redisServer =
            RedisServer.builder()
                .port(6370)
                .setting("maxmemory 128M")
                .build()
        redisServer?.start()
    }

    @PreDestroy
    fun stopRedis() {
        redisServer?.stop()
    }

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6370)
    }
}
