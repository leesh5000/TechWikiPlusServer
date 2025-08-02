package me.helloc.techwikiplus.service.user.infrastructure.id

import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator

class FakeIdGenerator : IdGenerator {
    private var counter = 0L

    override fun next(): String {
        return "test-id-${++counter}"
    }

    fun reset() {
        counter = 0L
    }
}
