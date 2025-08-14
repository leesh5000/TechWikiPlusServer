package me.helloc.techwikiplus.service.document.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Document
import me.helloc.techwikiplus.service.document.domain.model.DocumentId
import me.helloc.techwikiplus.service.document.domain.model.DocumentStatus
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.infrastructure.persistence.jpa.entity.DocumentEntity
import org.springframework.stereotype.Component

@Component
class DocumentEntityMapper {
    fun toDomain(entity: DocumentEntity): Document {
        return Document(
            id = DocumentId(entity.id),
            title = Title(entity.title),
            content = Content(entity.content),
            status = DocumentStatus.valueOf(entity.status),
            author = Author(entity.authorId),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toEntity(document: Document): DocumentEntity {
        return DocumentEntity(
            id = document.id.value,
            title = document.title.value,
            content = document.content.value,
            status = document.status.name,
            authorId = document.author.id,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
        )
    }
}
