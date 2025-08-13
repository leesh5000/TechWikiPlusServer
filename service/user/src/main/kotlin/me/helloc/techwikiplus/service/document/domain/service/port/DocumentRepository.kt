package me.helloc.techwikiplus.service.document.domain.service.port

import me.helloc.techwikiplus.service.document.domain.model.Document

interface DocumentRepository {
    fun save(document: Document): Document
}
