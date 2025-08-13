package me.helloc.techwikiplus.service.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.service.common.infrastructure.persistence.jpa.entity.DocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DocumentJpaRepository : JpaRepository<DocumentEntity, Long> {
    // Find documents by author
    fun findByAuthorId(authorId: Long): List<DocumentEntity>

    // Find documents by author with pagination
    fun findByAuthorId(
        authorId: Long,
        pageable: Pageable,
    ): Page<DocumentEntity>

    // Find documents by status
    fun findByStatus(status: String): List<DocumentEntity>

    // Find documents by author and status
    fun findByAuthorIdAndStatus(
        authorId: Long,
        status: String,
    ): List<DocumentEntity>

    // Search documents by title (using LIKE for now, can be upgraded to fulltext)
    @Query("SELECT d FROM DocumentEntity d WHERE d.title LIKE %:keyword%")
    fun searchByTitle(
        @Param("keyword") keyword: String,
    ): List<DocumentEntity>

    // Count documents by author
    fun countByAuthorId(authorId: Long): Long

    // Check if document exists for author
    fun existsByIdAndAuthorId(
        id: Long,
        authorId: Long,
    ): Boolean
}
