package me.helloc.techwikiplus.user.infrastructure.refreshtoken.redis

import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.redis.RedisKeyFormatter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRedisStore(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenStore {
    override fun store(
        userId: Long,
        refreshToken: String,
        ttl: Duration,
    ) {
        try {
            val tokenKey = RedisKeyFormatter.refreshTokenKey(refreshToken)
            val userKey = RedisKeyFormatter.userRefreshTokenKey(userId)

            // 기존 토큰이 있다면 무조건 정리 (토큰 로테이션)
            val existingToken = redisTemplate.opsForValue().get(userKey)
            existingToken?.let { oldToken ->
                val oldTokenKey = RedisKeyFormatter.refreshTokenKey(oldToken)
                redisTemplate.delete(oldTokenKey)
            }

            // 새 토큰 저장
            redisTemplate.opsForValue().set(tokenKey, userId.toString(), ttl)
            redisTemplate.opsForValue().set(userKey, refreshToken, ttl)
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }

    override fun exists(refreshToken: String): Boolean {
        return try {
            val key = RedisKeyFormatter.refreshTokenKey(refreshToken)
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }

    override fun invalidate(
        userId: Long?,
        refreshToken: String?,
    ) {
        when {
            userId != null && refreshToken == null -> invalidateByUserId(userId)
            refreshToken != null -> invalidateByToken(refreshToken)
            else -> throw IllegalArgumentException("Either userId or refreshToken must be provided")
        }
    }

    private fun invalidateByUserId(userId: Long) {
        try {
            val userKey = RedisKeyFormatter.userRefreshTokenKey(userId)
            val existingToken = redisTemplate.opsForValue().get(userKey)

            existingToken?.let {
                val tokenKey = RedisKeyFormatter.refreshTokenKey(it)
                redisTemplate.delete(tokenKey)
            }

            redisTemplate.delete(userKey)
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }

    private fun invalidateByToken(refreshToken: String) {
        try {
            val tokenKey = RedisKeyFormatter.refreshTokenKey(refreshToken)
            val storedUserId = redisTemplate.opsForValue().get(tokenKey)

            storedUserId?.let {
                val userKey = RedisKeyFormatter.userRefreshTokenKey(it.toLong())
                val currentToken = redisTemplate.opsForValue().get(userKey)
                // 현재 저장된 토큰과 무효화하려는 토큰이 같을 때만 user 키도 삭제
                // (토큰 로테이션으로 이미 새 토큰이 저장되었을 수 있음)
                if (currentToken == refreshToken) {
                    redisTemplate.delete(userKey)
                }
            }

            redisTemplate.delete(tokenKey)
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }
}
