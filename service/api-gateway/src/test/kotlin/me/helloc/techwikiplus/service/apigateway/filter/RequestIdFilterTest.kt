package me.helloc.techwikiplus.service.apigateway.filter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI

class RequestIdFilterTest : FunSpec({

    val filter = RequestIdFilter()

    test("Should generate Request ID when not present") {
        // Given
        val exchange = mockk<ServerWebExchange>()
        val request = mockk<ServerHttpRequest>()
        val response = mockk<ServerHttpResponse>()
        val chain = mockk<GatewayFilterChain>()
        val headers = HttpHeaders()
        val responseHeaders = HttpHeaders()
        val uri = URI.create("/api/v1/users/test")

        every { exchange.request } returns request
        every { exchange.response } returns response
        every { request.headers } returns headers
        every { request.uri } returns uri
        every { response.headers } returns responseHeaders
        every { request.mutate() } returns
            mockk {
                every { header(any(), any()) } returns this
                every { build() } returns request
            }
        every { exchange.mutate() } returns
            mockk {
                every { request(any<ServerHttpRequest>()) } returns this
                every { build() } returns exchange
            }
        every { chain.filter(any()) } returns Mono.empty()

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { chain.filter(any()) }
        responseHeaders[RequestIdFilter.REQUEST_ID_HEADER] shouldNotBe null
        responseHeaders[RequestIdFilter.CORRELATION_ID_HEADER] shouldNotBe null
        responseHeaders[RequestIdFilter.GATEWAY_REQUEST_ID_HEADER] shouldNotBe null
    }

    test("Should use existing Request ID when present") {
        // Given
        val existingRequestId = "existing-request-id"
        val exchange = mockk<ServerWebExchange>()
        val request = mockk<ServerHttpRequest>()
        val response = mockk<ServerHttpResponse>()
        val chain = mockk<GatewayFilterChain>()
        val headers = HttpHeaders()
        headers.add(RequestIdFilter.REQUEST_ID_HEADER, existingRequestId)
        val responseHeaders = HttpHeaders()
        val uri = URI.create("/api/v1/users/test")

        every { exchange.request } returns request
        every { exchange.response } returns response
        every { request.headers } returns headers
        every { request.uri } returns uri
        every { response.headers } returns responseHeaders
        every { request.mutate() } returns
            mockk {
                every { header(any(), any()) } returns this
                every { build() } returns request
            }
        every { exchange.mutate() } returns
            mockk {
                every { request(any<ServerHttpRequest>()) } returns this
                every { build() } returns exchange
            }
        every { chain.filter(any()) } returns Mono.empty()

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { chain.filter(any()) }
        responseHeaders[RequestIdFilter.REQUEST_ID_HEADER]?.get(0) shouldBe existingRequestId
    }

    test("Should have correct filter order") {
        filter.order shouldBe -2147483647 // HIGHEST_PRECEDENCE + 1
    }
})
