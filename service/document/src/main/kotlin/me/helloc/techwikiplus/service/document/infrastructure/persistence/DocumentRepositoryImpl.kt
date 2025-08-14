package me.helloc.techwikiplus.service.document.infrastructure.persistence

import me.helloc.techwikiplus.service.document.domain.model.Document
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentRepository
import me.helloc.techwikiplus.service.document.infrastructure.persistence.jpa.DocumentJpaRepository
import me.helloc.techwikiplus.service.document.infrastructure.persistence.jpa.mapper.DocumentEntityMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class DocumentRepositoryImpl(
    private val jpaRepository: DocumentJpaRepository,
    private val mapper: DocumentEntityMapper,
) : DocumentRepository {
    @Transactional
    override fun save(document: Document): Document {
        val entity = mapper.toEntity(document)
        val savedEntity = jpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }
}
