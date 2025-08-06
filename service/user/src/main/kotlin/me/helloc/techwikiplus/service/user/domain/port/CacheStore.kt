package me.helloc.techwikiplus.service.user.domain.port

import java.time.Duration

interface CacheStore {
    /**
     * 캐시에서 값을 가져옵니다.
     *
     * @param key 캐시 키
     * @return 캐시된 값 또는 null
     */
    fun get(key: String): String?

    /**
     * 캐시에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     * @param ttl 캐시의 유효 기간. 기본값은 0으로, 무제한 캐시를 의미합니다.
     */
    fun put(
        key: String,
        value: String,
        ttl: Duration = Duration.ZERO,
    )

    /**
     * 캐시에서 값을 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     */
    fun delete(key: String)
}
