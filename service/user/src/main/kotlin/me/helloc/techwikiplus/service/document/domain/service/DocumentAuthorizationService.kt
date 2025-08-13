package me.helloc.techwikiplus.service.document.domain.service

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode
import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentAuthorizationPort
import org.springframework.stereotype.Service

@Service
class DocumentAuthorizationService(
    private val documentAuthorizationPort: DocumentAuthorizationPort,
) {
    fun canWriteOrThrows(author: Author) {
        val loginUser = documentAuthorizationPort.requireAuthenticated()
        if (loginUser.id != author.id) {
            throw DocumentDomainException(
                DocumentErrorCode.NOT_AUTHOR_OF_DOCUMENT,
                arrayOf(loginUser.id, author.id),
            )
        }
    }
}
