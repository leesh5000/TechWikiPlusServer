package me.helloc.techwikiplus.service.user.adapter.outbound.id

import me.helloc.techwikiplus.service.user.application.port.outbound.IdGenerator

class FakeIdGenerator : IdGenerator {
    private var counter = 0L

    override fun next(): String {
        return "test-id-${++counter}"
    }

    fun reset() {
        counter = 0L
    }
}
