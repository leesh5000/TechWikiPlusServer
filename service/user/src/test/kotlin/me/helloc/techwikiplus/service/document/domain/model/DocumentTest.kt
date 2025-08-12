package me.helloc.techwikiplus.service.document.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class DocumentTest : FunSpec({

    context("Document 생성") {
        test("유효한 값으로 Document를 생성할 수 있다") {
            // given
            val id = DocumentId(123L)
            val title = Title("테스트 문서")
            val content = Content("테스트 내용입니다.")
            val status = DocumentStatus.DRAFT
            val author = Author(456L)
            val now = Instant.now()

            // when
            val document =
                Document(
                    id = id,
                    title = title,
                    content = content,
                    status = status,
                    author = author,
                    createdAt = now,
                    updatedAt = now,
                )

            // then
            document.id shouldBe id
            document.title shouldBe title
            document.content shouldBe content
            document.status shouldBe status
            document.author shouldBe author
            document.createdAt shouldBe now
            document.updatedAt shouldBe now
        }

        test("create 팩토리 메서드로 Document를 생성할 수 있다") {
            // given
            val id = DocumentId(789L)
            val title = Title("팩토리 메서드 테스트")
            val content = Content("팩토리 메서드로 생성된 문서")
            val status = DocumentStatus.IN_REVIEW
            val author = Author(999L)
            val createdAt = Instant.now()

            // when
            val document =
                Document.create(
                    id = id,
                    title = title,
                    content = content,
                    status = status,
                    author = author,
                    createdAt = createdAt,
                )

            // then
            document.id shouldBe id
            document.title shouldBe title
            document.content shouldBe content
            document.status shouldBe status
            document.author shouldBe author
            document.createdAt shouldBe createdAt
            document.updatedAt shouldBe createdAt // modifiedAt 기본값이 createdAt
        }

        test("create 팩토리 메서드에서 modifiedAt을 별도로 지정할 수 있다") {
            // given
            val id = DocumentId(111L)
            val title = Title("수정 시간 테스트")
            val content = Content("수정 시간이 다른 문서")
            val status = DocumentStatus.REVIEWED
            val author = Author(222L)
            val createdAt = Instant.now()
            val modifiedAt = createdAt.plusSeconds(3600) // 1시간 후

            // when
            val document =
                Document.create(
                    id = id,
                    title = title,
                    content = content,
                    status = status,
                    author = author,
                    createdAt = createdAt,
                    modifiedAt = modifiedAt,
                )

            // then
            document.createdAt shouldBe createdAt
            document.updatedAt shouldBe modifiedAt
            document.updatedAt shouldNotBe document.createdAt
        }
    }

    context("Document copy") {
        test("copy 메서드로 일부 필드만 변경할 수 있다") {
            // given
            val original =
                Document(
                    id = DocumentId(1001L),
                    title = Title("원본 제목"),
                    content = Content("원본 내용"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2001L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val newTitle = Title("수정된 제목")
            val newContent = Content("수정된 내용")

            // when
            val copied =
                original.copy(
                    title = newTitle,
                    content = newContent,
                )

            // then
            copied.id shouldBe original.id
            copied.title shouldBe newTitle
            copied.content shouldBe newContent
            copied.status shouldBe original.status
            copied.author shouldBe original.author
            copied.createdAt shouldBe original.createdAt
            copied.updatedAt shouldBe original.updatedAt
        }

        test("copy 메서드로 status를 변경할 수 있다") {
            // given
            val original =
                Document(
                    id = DocumentId(1002L),
                    title = Title("상태 변경 테스트"),
                    content = Content("상태가 변경될 문서"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2002L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // when
            val inReview = original.copy(status = DocumentStatus.IN_REVIEW)
            val reviewed = original.copy(status = DocumentStatus.REVIEWED)

            // then
            inReview.status shouldBe DocumentStatus.IN_REVIEW
            reviewed.status shouldBe DocumentStatus.REVIEWED
            original.status shouldBe DocumentStatus.DRAFT // 원본은 변경되지 않음
        }

        test("copy 메서드로 updatedAt을 변경할 수 있다") {
            // given
            val createdAt = Instant.now()
            val original =
                Document(
                    id = DocumentId(1003L),
                    title = Title("시간 변경 테스트"),
                    content = Content("시간이 변경될 문서"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2003L),
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
            val newUpdatedAt = createdAt.plusSeconds(7200) // 2시간 후

            // when
            val updated = original.copy(updatedAt = newUpdatedAt)

            // then
            updated.updatedAt shouldBe newUpdatedAt
            updated.createdAt shouldBe createdAt
            original.updatedAt shouldBe createdAt // 원본은 변경되지 않음
        }
    }

    context("Document equals와 hashCode") {
        test("동일한 id를 가진 Document는 equals가 true를 반환한다") {
            // given
            val id = DocumentId(1004L)
            val document1 =
                Document(
                    id = id,
                    title = Title("제목1"),
                    content = Content("내용1"),
                    status = DocumentStatus.DRAFT,
                    author = Author(3001L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val document2 =
                Document(
                    id = id,
                    title = Title("제목2"),
                    content = Content("내용2"),
                    status = DocumentStatus.IN_REVIEW,
                    author = Author(3002L),
                    createdAt = Instant.now().plusSeconds(1000),
                    updatedAt = Instant.now().plusSeconds(2000),
                )

            // when & then
            (document1 == document2) shouldBe true
            document1.equals(document2) shouldBe true
        }

        test("다른 id를 가진 Document는 equals가 false를 반환한다") {
            // given
            val now = Instant.now()
            val document1 =
                Document(
                    id = DocumentId(1L),
                    title = Title("동일한 제목"),
                    content = Content("동일한 내용"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2006L),
                    createdAt = now,
                    updatedAt = now,
                )
            val document2 =
                Document(
                    id = DocumentId(2L),
                    title = Title("동일한 제목"),
                    content = Content("동일한 내용"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2006L),
                    createdAt = now,
                    updatedAt = now,
                )

            // when & then
            (document1 == document2) shouldBe false
            document1.equals(document2) shouldBe false
        }

        test("동일한 id를 가진 Document는 동일한 hashCode를 반환한다") {
            // given
            val id = DocumentId(1005L)
            val document1 =
                Document(
                    id = id,
                    title = Title("제목1"),
                    content = Content("내용1"),
                    status = DocumentStatus.DRAFT,
                    author = Author(4001L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val document2 =
                Document(
                    id = id,
                    title = Title("제목2"),
                    content = Content("내용2"),
                    status = DocumentStatus.IN_REVIEW,
                    author = Author(4002L),
                    createdAt = Instant.now().plusSeconds(1000),
                    updatedAt = Instant.now().plusSeconds(2000),
                )

            // when & then
            document1.hashCode() shouldBe document2.hashCode()
        }

        test("자기 자신과의 equals는 true를 반환한다") {
            // given
            val document =
                Document(
                    id = DocumentId(1006L),
                    title = Title("자기 참조 테스트"),
                    content = Content("자기 자신과 비교"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2007L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // when & then
            (document == document) shouldBe true
            document.equals(document) shouldBe true
        }

        test("null과의 equals는 false를 반환한다") {
            // given
            val document =
                Document(
                    id = DocumentId(1007L),
                    title = Title("null 비교 테스트"),
                    content = Content("null과 비교"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2008L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // when & then
            document.equals(null) shouldBe false
        }

        test("다른 타입과의 equals는 false를 반환한다") {
            // given
            val document =
                Document(
                    id = DocumentId(1008L),
                    title = Title("타입 비교 테스트"),
                    content = Content("다른 타입과 비교"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2009L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val otherType = "Not a Document"

            // when & then
            document.equals(otherType) shouldBe false
        }
    }

    context("Document 상태 관리") {
        test("DRAFT 상태의 문서를 생성할 수 있다") {
            // given & when
            val document =
                Document(
                    id = DocumentId(1009L),
                    title = Title("초안 문서"),
                    content = Content("초안 내용"),
                    status = DocumentStatus.DRAFT,
                    author = Author(2010L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // then
            document.status shouldBe DocumentStatus.DRAFT
        }

        test("IN_REVIEW 상태의 문서를 생성할 수 있다") {
            // given & when
            val document =
                Document(
                    id = DocumentId(1010L),
                    title = Title("검토중 문서"),
                    content = Content("검토중 내용"),
                    status = DocumentStatus.IN_REVIEW,
                    author = Author(2011L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // then
            document.status shouldBe DocumentStatus.IN_REVIEW
        }

        test("REVIEWED 상태의 문서를 생성할 수 있다") {
            // given & when
            val document =
                Document(
                    id = DocumentId(1011L),
                    title = Title("검토완료 문서"),
                    content = Content("검토완료 내용"),
                    status = DocumentStatus.REVIEWED,
                    author = Author(2012L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            // then
            document.status shouldBe DocumentStatus.REVIEWED
        }
    }
})
