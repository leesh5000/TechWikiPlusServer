package me.helloc.techwikiplus.user.infrastructure.refreshtoken.redis

import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRedisStore(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenStore {
    companion object {
        private const val REFRESH_TOKEN_KEY_FORMAT = "refresh_token:%s"
        private const val USER_REFRESH_TOKEN_KEY_FORMAT = "user_refresh_token:%d"
    }

    override fun store(
        userId: Long,
        refreshToken: String,
        ttl: Duration,
    ) {
        // 기존 토큰 무효화
        invalidate(userId)

        // 새 토큰 저장
        val tokenKey = String.format(REFRESH_TOKEN_KEY_FORMAT, refreshToken)
        val userKey = String.format(USER_REFRESH_TOKEN_KEY_FORMAT, userId)

        redisTemplate.opsForValue().set(tokenKey, userId.toString(), ttl)
        redisTemplate.opsForValue().set(userKey, refreshToken, ttl)
    }

    override fun exists(refreshToken: String): Boolean {
        val key = String.format(REFRESH_TOKEN_KEY_FORMAT, refreshToken)
        return redisTemplate.hasKey(key)
    }

    override fun invalidate(userId: Long) {
        val userKey = String.format(USER_REFRESH_TOKEN_KEY_FORMAT, userId)
        val existingToken = redisTemplate.opsForValue().get(userKey)

        existingToken?.let {
            val tokenKey = String.format(REFRESH_TOKEN_KEY_FORMAT, it)
            redisTemplate.delete(tokenKey)
        }

        redisTemplate.delete(userKey)
    }

    override fun invalidateToken(refreshToken: String) {
        val tokenKey = String.format(REFRESH_TOKEN_KEY_FORMAT, refreshToken)
        val userId = redisTemplate.opsForValue().get(tokenKey)

        userId?.let {
            val userKey = String.format(USER_REFRESH_TOKEN_KEY_FORMAT, it.toLong())
            redisTemplate.delete(userKey)
        }

        redisTemplate.delete(tokenKey)
    }
}
