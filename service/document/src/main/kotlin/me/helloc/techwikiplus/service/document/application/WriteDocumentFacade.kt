package me.helloc.techwikiplus.service.document.application

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.domain.service.DocumentAuthorizationService
import me.helloc.techwikiplus.service.document.domain.service.DocumentRegister
import me.helloc.techwikiplus.service.document.interfaces.web.port.WriteDocumentUseCase
import org.springframework.stereotype.Component

@Transactional
@Component
class WriteDocumentFacade(
    private val documentAuthorizationService: DocumentAuthorizationService,
    private val documentRegister: DocumentRegister,
) : WriteDocumentUseCase {
    override fun handle(
        title: Title,
        content: Content,
        author: Author,
    ) {
        documentAuthorizationService.canWriteOrThrows(author)
        documentRegister.insert(title, content, author)
    }
}
