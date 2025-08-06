package me.helloc.techwikiplus.service.user.domain.port

interface IdGenerator {
    fun next(): String
}
