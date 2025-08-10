package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.model.value.UserId

interface IdGenerator {
    fun next(): UserId
}
