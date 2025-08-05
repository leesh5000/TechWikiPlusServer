package me.helloc.techwikiplus.service.user.application.port.outbound

interface IdGenerator {
    fun next(): String
}
