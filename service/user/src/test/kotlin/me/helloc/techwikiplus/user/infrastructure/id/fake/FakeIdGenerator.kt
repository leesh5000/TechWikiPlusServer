package me.helloc.techwikiplus.user.infrastructure.id.fake

import me.helloc.techwikiplus.user.domain.service.IdGenerator

class FakeIdGenerator(private var currentId: Long = 1L) : IdGenerator {
    override fun next(): Long {
        return currentId++
    }

    fun reset(startId: Long = 1L) {
        currentId = startId
    }
}