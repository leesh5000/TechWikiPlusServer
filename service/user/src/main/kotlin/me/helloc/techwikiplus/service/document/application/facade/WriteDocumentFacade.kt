package me.helloc.techwikiplus.service.document.application.facade

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.interfaces.web.port.WriteDocumentUseCase
import me.helloc.techwikiplus.service.user.domain.service.port.AuthorizationPort
import org.springframework.stereotype.Component

@Transactional
@Component
class WriteDocumentFacade(
    private val authorizationPort: AuthorizationPort,
) : WriteDocumentUseCase {
    override fun execute(
        title: Title,
        content: Content,
        author: Author,
    ) {
        TODO("Not yet implemented")
    }
}
