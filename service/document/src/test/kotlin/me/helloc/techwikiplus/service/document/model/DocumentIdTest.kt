package me.helloc.techwikiplus.service.document.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode
import kotlin.collections.get

class DocumentIdTest : FunSpec({

    context("DocumentId 생성") {
        test("유효한 양수 값으로 DocumentId를 생성할 수 있다") {
            // given
            val validId = 1L

            // when
            val documentId = DocumentId(validId)

            // then
            documentId.value shouldBe validId
        }

        test("큰 양수 값으로 DocumentId를 생성할 수 있다") {
            // given
            val largeId = 9223372036854775807L // Long.MAX_VALUE

            // when
            val documentId = DocumentId(largeId)

            // then
            documentId.value shouldBe largeId
        }

        test("Snowflake ID 형식의 값으로 DocumentId를 생성할 수 있다") {
            // given
            val snowflakeId = 1234567890123456789L

            // when
            val documentId = DocumentId(snowflakeId)

            // then
            documentId.value shouldBe snowflakeId
        }
    }

    context("DocumentId 생성 실패") {
        test("0으로 DocumentId를 생성하면 예외가 발생한다") {
            // given
            val zeroId = 0L

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId(zeroId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("음수 값으로 DocumentId를 생성하면 예외가 발생한다") {
            // given
            val negativeId = -1L

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId(negativeId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("큰 음수 값으로 DocumentId를 생성하면 예외가 발생한다") {
            // given
            val largeNegativeId = Long.MIN_VALUE

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId(largeNegativeId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }
    }

    context("from 메서드") {
        test("유효한 문자열로 DocumentId를 생성할 수 있다") {
            // given
            val validIdString = "123"

            // when
            val documentId = DocumentId.from(validIdString)

            // then
            documentId.value shouldBe 123L
        }

        test("큰 숫자 문자열로 DocumentId를 생성할 수 있다") {
            // given
            val largeIdString = "9223372036854775807"

            // when
            val documentId = DocumentId.from(largeIdString)

            // then
            documentId.value shouldBe 9223372036854775807L
        }

        test("Snowflake ID 문자열로 DocumentId를 생성할 수 있다") {
            // given
            val snowflakeIdString = "1234567890123456789"

            // when
            val documentId = DocumentId.from(snowflakeIdString)

            // then
            documentId.value shouldBe 1234567890123456789L
        }

        test("숫자가 아닌 문자열로 생성하면 예외가 발생한다") {
            // given
            val invalidString = "abc"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(invalidString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe invalidString
            exception.cause shouldNotBe null
        }

        test("빈 문자열로 생성하면 예외가 발생한다") {
            // given
            val emptyString = ""

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(emptyString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe emptyString
            exception.cause shouldNotBe null
        }

        test("공백 문자열로 생성하면 예외가 발생한다") {
            // given
            val blankString = "   "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(blankString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe blankString
            exception.cause shouldNotBe null
        }

        test("특수문자가 포함된 문자열로 생성하면 예외가 발생한다") {
            // given
            val specialCharString = "123@456"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(specialCharString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe specialCharString
            exception.cause shouldNotBe null
        }

        test("소수점이 포함된 문자열로 생성하면 예외가 발생한다") {
            // given
            val decimalString = "123.456"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(decimalString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe decimalString
            exception.cause shouldNotBe null
        }

        test("0 문자열로 생성하면 예외가 발생한다") {
            // given
            val zeroString = "0"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(zeroString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("음수 문자열로 생성하면 예외가 발생한다") {
            // given
            val negativeString = "-123"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(negativeString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("Long 범위를 초과하는 문자열로 생성하면 예외가 발생한다") {
            // given
            val overflowString = "9223372036854775808" // Long.MAX_VALUE + 1

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    DocumentId.from(overflowString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT
            exception.params[0] shouldBe overflowString
            exception.cause shouldNotBe null
        }
    }

    context("equals와 hashCode") {
        test("같은 값을 가진 DocumentId 객체는 동등하다") {
            // given
            val documentId1 = DocumentId(123L)
            val documentId2 = DocumentId(123L)

            // when & then
            documentId1 shouldBe documentId2
            documentId1.hashCode() shouldBe documentId2.hashCode()
        }

        test("다른 값을 가진 DocumentId 객체는 동등하지 않다") {
            // given
            val documentId1 = DocumentId(123L)
            val documentId2 = DocumentId(456L)

            // when & then
            documentId1 shouldNotBe documentId2
            documentId1.hashCode() shouldNotBe documentId2.hashCode()
        }

        test("같은 객체 참조는 동등하다") {
            // given
            val documentId = DocumentId(123L)

            // when & then
            documentId shouldBe documentId
            documentId.hashCode() shouldBe documentId.hashCode()
        }

        test("null과 비교하면 동등하지 않다") {
            // given
            val documentId = DocumentId(123L)

            // when & then
            (documentId.equals(null)) shouldBe false
        }

        test("다른 타입의 객체와 비교하면 동등하지 않다") {
            // given
            val documentId = DocumentId(123L)
            val otherObject = 123L

            // when & then
            (documentId.equals(otherObject)) shouldBe false
        }

        test("문자열 객체와 비교하면 동등하지 않다") {
            // given
            val documentId = DocumentId(123L)
            val stringObject = "123"

            // when & then
            (documentId.equals(stringObject)) shouldBe false
        }
    }

    context("toString") {
        test("DocumentId 객체의 문자열 표현을 반환한다") {
            // given
            val idValue = 123L
            val documentId = DocumentId(idValue)

            // when
            val result = documentId.toString()

            // then
            result shouldBe "123"
        }

        test("큰 값의 DocumentId 객체의 문자열 표현을 반환한다") {
            // given
            val largeId = 9223372036854775807L
            val documentId = DocumentId(largeId)

            // when
            val result = documentId.toString()

            // then
            result shouldBe "9223372036854775807"
        }
    }

    context("경계값 테스트") {
        test("1로 DocumentId를 생성할 수 있다 (최소 유효값)") {
            // given
            val minValidId = 1L

            // when
            val documentId = DocumentId(minValidId)

            // then
            documentId.value shouldBe minValidId
        }

        test("Long.MAX_VALUE로 DocumentId를 생성할 수 있다") {
            // given
            val maxId = Long.MAX_VALUE

            // when
            val documentId = DocumentId(maxId)

            // then
            documentId.value shouldBe maxId
        }

        test("문자열 '1'로 DocumentId를 생성할 수 있다") {
            // given
            val minValidString = "1"

            // when
            val documentId = DocumentId.from(minValidString)

            // then
            documentId.value shouldBe 1L
        }
    }

    context("통합 시나리오 테스트") {
        test("from 메서드와 생성자가 동일한 결과를 반환한다") {
            // given
            val idValue = 12345L
            val idString = "12345"

            // when
            val documentIdFromConstructor = DocumentId(idValue)
            val documentIdFromString = DocumentId.from(idString)

            // then
            documentIdFromConstructor shouldBe documentIdFromString
            documentIdFromConstructor.hashCode() shouldBe documentIdFromString.hashCode()
            documentIdFromConstructor.toString() shouldBe documentIdFromString.toString()
        }

        test("여러 DocumentId 객체를 Set에 저장할 수 있다") {
            // given
            val id1 = DocumentId(1L)
            val id2 = DocumentId(2L)
            val id3 = DocumentId(1L) // id1과 같은 값

            // when
            val idSet = setOf(id1, id2, id3)

            // then
            idSet.size shouldBe 2
            idSet.contains(id1) shouldBe true
            idSet.contains(id2) shouldBe true
        }

        test("DocumentId를 Map의 키로 사용할 수 있다") {
            // given
            val id1 = DocumentId(1L)
            val id2 = DocumentId(2L)
            val id3 = DocumentId(1L) // id1과 같은 값

            // when
            val idMap = mutableMapOf<DocumentId, String>()
            idMap[id1] = "Document 1"
            idMap[id2] = "Document 2"
            idMap[id3] = "Document 1 Updated"

            // then
            idMap.size shouldBe 2
            idMap[id1] shouldBe "Document 1 Updated"
            idMap[id2] shouldBe "Document 2"
        }
    }
})
