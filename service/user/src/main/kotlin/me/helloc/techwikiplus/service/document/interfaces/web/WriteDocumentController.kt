package me.helloc.techwikiplus.service.document.interfaces.web

import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.interfaces.web.port.WriteDocumentUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class WriteDocumentController(
    private val useCase: WriteDocumentUseCase,
) {
    @PostMapping("/api/v1/documents", consumes = ["application/json"])
    fun writeDocument(
        @RequestBody request: Request,
    ): ResponseEntity<Void> {
        useCase.execute(
            Title(request.title),
            Content(request.content),
            Author.from(request.authorId),
        )
        return ResponseEntity.ok().build()
    }

    data class Request(
        val title: String,
        val content: String,
        val authorId: String,
    )
}
