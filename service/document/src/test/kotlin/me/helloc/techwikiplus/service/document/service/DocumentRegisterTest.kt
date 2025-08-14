package me.helloc.techwikiplus.service.document.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode
import me.helloc.techwikiplus.service.document.domain.model.Author
import me.helloc.techwikiplus.service.document.domain.model.Content
import me.helloc.techwikiplus.service.document.domain.model.DocumentId
import me.helloc.techwikiplus.service.document.domain.model.DocumentStatus
import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.service.document.infrastructure.FakeDocumentIdGenerator
import me.helloc.techwikiplus.service.document.infrastructure.FakeDocumentRepository
import java.time.Instant
import kotlin.collections.get

class DocumentRegisterTest : FunSpec({

    lateinit var documentRegister: DocumentRegister
    lateinit var clockHolder: FakeClockHolder
    lateinit var idGenerator: FakeDocumentIdGenerator
    lateinit var repository: FakeDocumentRepository

    beforeEach {
        clockHolder = FakeClockHolder(Instant.parse("2025-01-13T10:00:00Z"))
        idGenerator = FakeDocumentIdGenerator()
        repository = FakeDocumentRepository()
        documentRegister = DocumentRegister(clockHolder, idGenerator, repository)
    }

    afterEach {
        repository.clear()
        idGenerator.reset()
    }

    context("insert 메서드는") {
        test("유효한 정보로 문서를 성공적으로 등록한다") {
            // given
            val title = Title("테스트 문서 제목")
            val content = Content("테스트 문서 내용입니다. 이것은 문서의 본문입니다.")
            val author = Author(1000001L)

            // when
            documentRegister.insert(title, content, author)

            // then
            val savedDocuments = repository.getAll()
            savedDocuments.size shouldBe 1

            val savedDocument = savedDocuments.first()
            savedDocument.id shouldBe DocumentId(2000000L)
            savedDocument.title shouldBe title
            savedDocument.content shouldBe content
            savedDocument.author shouldBe author
            savedDocument.status shouldBe DocumentStatus.DRAFT
            savedDocument.createdAt shouldBe Instant.parse("2025-01-13T10:00:00Z")
            savedDocument.updatedAt shouldBe Instant.parse("2025-01-13T10:00:00Z")
        }

        test("여러 문서를 순차적으로 등록할 수 있다") {
            // given
            val title1 = Title("첫 번째 문서")
            val content1 = Content("첫 번째 문서의 내용입니다.")
            val author1 = Author(1000001L)

            val title2 = Title("두 번째 문서")
            val content2 = Content("두 번째 문서의 내용입니다.")
            val author2 = Author(1000002L)

            // when
            documentRegister.insert(title1, content1, author1)

            // 시간 경과 시뮬레이션
            clockHolder.advanceTimeBySeconds(60)

            documentRegister.insert(title2, content2, author2)

            // then
            val savedDocuments = repository.getAll()
            savedDocuments.size shouldBe 2

            val document1 = repository.findById(DocumentId(2000000L))
            document1 shouldNotBe null
            document1?.title shouldBe title1
            document1?.content shouldBe content1
            document1?.author shouldBe author1
            document1?.createdAt shouldBe Instant.parse("2025-01-13T10:00:00Z")

            val document2 = repository.findById(DocumentId(2000001L))
            document2 shouldNotBe null
            document2?.title shouldBe title2
            document2?.content shouldBe content2
            document2?.author shouldBe author2
            document2?.createdAt shouldBe Instant.parse("2025-01-13T10:01:00Z")
        }

        test("동일한 제목으로 여러 문서를 등록할 수 있다") {
            // given
            val sameTitle = Title("동일한 제목")
            val content1 = Content("첫 번째 문서의 내용")
            val content2 = Content("두 번째 문서의 내용")
            val author = Author(1000001L)

            // when
            documentRegister.insert(sameTitle, content1, author)
            documentRegister.insert(sameTitle, content2, author)

            // then
            val savedDocuments = repository.getAll()
            savedDocuments.size shouldBe 2

            savedDocuments.all { it.title == sameTitle } shouldBe true
            savedDocuments[0].content shouldBe content1
            savedDocuments[1].content shouldBe content2
        }

        test("서로 다른 작성자가 문서를 등록할 수 있다") {
            // given
            val title = Title("공동 작업 문서")
            val content = Content("여러 작성자가 작성할 수 있는 문서")
            val author1 = Author(1000001L)
            val author2 = Author(1000002L)

            // when
            documentRegister.insert(title, content, author1)
            documentRegister.insert(title, content, author2)

            // then
            val savedDocuments = repository.getAll()
            savedDocuments.size shouldBe 2

            val doc1 = savedDocuments[0]
            val doc2 = savedDocuments[1]

            doc1.author shouldBe author1
            doc2.author shouldBe author2
            doc1.id shouldNotBe doc2.id
        }

        test("긴 제목과 내용으로 문서를 등록할 수 있다") {
            // given
            val longTitle = Title("a".repeat(200)) // 최대 길이 제목
            val longContent = Content("b".repeat(50000)) // 최대 길이 내용
            val author = Author(1000001L)

            // when
            documentRegister.insert(longTitle, longContent, author)

            // then
            val savedDocument = repository.findById(DocumentId(2000000L))
            savedDocument shouldNotBe null
            savedDocument?.title?.value?.length shouldBe 200
            savedDocument?.content?.value?.length shouldBe 50000
        }

        test("특수문자를 포함한 제목으로 문서를 등록할 수 있다") {
            // given
            val titleWithSpecialChars = Title("Spring Boot 3.0 - @RestController 사용법 #1")
            val content = Content("특수문자를 포함한 제목 테스트")
            val author = Author(1000001L)

            // when
            documentRegister.insert(titleWithSpecialChars, content, author)

            // then
            val savedDocument = repository.findById(DocumentId(2000000L))
            savedDocument shouldNotBe null
            savedDocument?.title shouldBe titleWithSpecialChars
        }

        test("빈 제목으로 문서를 등록하면 예외가 발생한다") {
            // given
            val content = Content("유효한 내용")
            val author = Author(1000001L)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title("   ") // 공백만 있는 제목
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_TITLE

            // 저장소에 문서가 저장되지 않았는지 확인
            repository.size() shouldBe 0
        }

        test("빈 내용으로 문서를 등록하면 예외가 발생한다") {
            // given
            val title = Title("유효한 제목")
            val author = Author(1000001L)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content("   ") // 공백만 있는 내용
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_CONTENT

            // 저장소에 문서가 저장되지 않았는지 확인
            repository.size() shouldBe 0
        }

        test("유효하지 않은 작성자 ID로 문서를 등록하면 예외가 발생한다") {
            // given
            val title = Title("유효한 제목")
            val content = Content("유효한 내용")

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author(0) // 0 또는 음수 ID
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT

            // 저장소에 문서가 저장되지 않았는지 확인
            repository.size() shouldBe 0
        }

        test("생성된 문서의 초기 상태는 DRAFT이다") {
            // given
            val title = Title("초안 문서")
            val content = Content("이 문서는 초안 상태로 생성됩니다.")
            val author = Author(1000001L)

            // when
            documentRegister.insert(title, content, author)

            // then
            val savedDocument = repository.findById(DocumentId(2000000L))
            savedDocument shouldNotBe null
            savedDocument?.status shouldBe DocumentStatus.DRAFT
        }

        test("문서 생성 시 createdAt과 updatedAt이 동일한 시간으로 설정된다") {
            // given
            val title = Title("시간 테스트 문서")
            val content = Content("생성 시간과 수정 시간 테스트")
            val author = Author(1000001L)
            val expectedTime = Instant.parse("2025-01-13T10:00:00Z")

            // when
            documentRegister.insert(title, content, author)

            // then
            val savedDocument = repository.findById(DocumentId(2000000L))
            savedDocument shouldNotBe null
            savedDocument?.createdAt shouldBe expectedTime
            savedDocument?.updatedAt shouldBe expectedTime
            savedDocument?.createdAt shouldBe savedDocument?.updatedAt
        }

        test("ID 생성기가 순차적으로 ID를 생성한다") {
            // given
            val title = Title("ID 테스트 문서")
            val content = Content("ID 생성 테스트")
            val author = Author(1000001L)

            // when
            repeat(3) { index ->
                documentRegister.insert(
                    Title("문서 ${index + 1}"),
                    content,
                    author,
                )
            }

            // then
            repository.findById(DocumentId(2000000L)) shouldNotBe null
            repository.findById(DocumentId(2000001L)) shouldNotBe null
            repository.findById(DocumentId(2000002L)) shouldNotBe null
            repository.size() shouldBe 3
        }
    }
})
