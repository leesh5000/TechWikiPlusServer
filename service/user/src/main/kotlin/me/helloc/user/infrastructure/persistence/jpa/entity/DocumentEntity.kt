package me.helloc.techwikiplus.service.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "documents",
    indexes = [
        Index(name = "idx_author_id", columnList = "author_id"),
        Index(name = "idx_status", columnList = "status"),
        Index(name = "idx_author_status", columnList = "author_id,status"),
        Index(name = "idx_created_at", columnList = "created_at"),
    ],
)
open class DocumentEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "title", nullable = false, length = 200)
    open val title: String,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    open val content: String,
    @Column(name = "status", nullable = false, length = 20)
    open val status: String = "DRAFT",
    @Column(name = "author_id", nullable = false, columnDefinition = "BIGINT")
    open val authorId: Long,
    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    open val updatedAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        title = "",
        content = "",
        status = "DRAFT",
        authorId = 0L,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentEntity) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "DocumentEntity(id='$id', title='$title', status='$status', " +
            "authorId='$authorId', createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}
