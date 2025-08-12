package me.helloc.techwikiplus.service.document.interfaces.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WriteDocumentController {

    @PostMapping("/api/v1/documents", consumes = ["application/json"])
    fun writeDocument(

    ): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }

    data class Request(
        val title: String,
        val content: String,
        val authorId: String,
    ) {

    }
}