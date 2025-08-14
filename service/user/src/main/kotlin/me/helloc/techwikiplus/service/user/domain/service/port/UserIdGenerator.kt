package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.UserId

interface UserIdGenerator {
    fun next(): UserId
}
