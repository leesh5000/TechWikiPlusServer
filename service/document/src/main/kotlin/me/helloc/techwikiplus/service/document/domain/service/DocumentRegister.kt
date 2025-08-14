package me.helloc.techwikiplus.service.document.domain.service

import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Document
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentIdGenerator
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentRepository
import org.springframework.stereotype.Service

@Service
class DocumentRegister(
    private val clockHolder: ClockHolder,
    private val idGenerator: DocumentIdGenerator,
    private val repository: DocumentRepository,
) {
    fun insert(
        title: Title,
        content: Content,
        author: Author,
    ) {
        val now = clockHolder.now()
        val document =
            Document.create(
                id = idGenerator.next(),
                title = title,
                content = content,
                author = author,
                createdAt = now,
            )
        repository.save(document)
    }
}
