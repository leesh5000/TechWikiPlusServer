package me.helloc.techwikiplus.service.user.domain.service.port

interface IdGenerator {
    fun next(): String
}
