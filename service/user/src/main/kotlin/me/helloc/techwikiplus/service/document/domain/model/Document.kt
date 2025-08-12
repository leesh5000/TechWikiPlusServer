package me.helloc.techwikiplus.service.document.domain.model

import java.time.Instant

class Document(
    val id: DocumentId,
    val title: Title,
    val content: Content,
    val status: DocumentStatus,
    val author: Author,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        // DocumentId validation is already done in the DocumentId value object
    }

    fun copy(
        id: DocumentId = this.id,
        title: Title = this.title,
        content: Content = this.content,
        status: DocumentStatus = this.status,
        author: Author = this.author,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt,
    ): Document {
        return Document(
            id = id,
            title = title,
            content = content,
            status = status,
            author = author,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Document) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun create(
            id: DocumentId,
            title: Title,
            content: Content,
            status: DocumentStatus,
            author: Author,
            createdAt: Instant,
            modifiedAt: Instant = createdAt,
        ): Document {
            return Document(
                id = id,
                title = title,
                content = content,
                status = status,
                author = author,
                createdAt = createdAt,
                updatedAt = modifiedAt,
            )
        }
    }
}
