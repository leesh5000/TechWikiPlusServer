package me.helloc.techwikiplus.service.document.domain.service.port

import me.helloc.techwikiplus.service.document.domain.model.DocumentId

interface DocumentIdGenerator {
    fun next(): DocumentId
}
