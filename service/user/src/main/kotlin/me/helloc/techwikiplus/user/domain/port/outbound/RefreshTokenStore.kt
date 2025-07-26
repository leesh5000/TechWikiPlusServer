package me.helloc.techwikiplus.user.domain.port.outbound

import java.time.Duration

interface RefreshTokenStore {
    fun store(
        userId: Long,
        refreshToken: String,
        ttl: Duration,
    )

    fun exists(refreshToken: String): Boolean

    fun invalidate(
        userId: Long? = null,
        refreshToken: String? = null,
    )
}
