package me.helloc.techwikiplus.service.document.interfaces.web.port

import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Title

interface WriteDocumentUseCase {
    fun execute(
        title: Title,
        content: Content,
        author: Author,
    )
}
