package me.helloc.techwikiplus.service.document.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class AuthorTest : FunSpec({

    context("Author 생성") {
        test("유효한 양수 값으로 Author를 생성할 수 있다") {
            // given
            val validId = 1L

            // when
            val author = Author(validId)

            // then
            author.id shouldBe validId
        }

        test("큰 양수 값으로 Author를 생성할 수 있다") {
            // given
            val largeId = 9223372036854775807L // Long.MAX_VALUE

            // when
            val author = Author(largeId)

            // then
            author.id shouldBe largeId
        }

        test("Snowflake ID 형식의 값으로 Author를 생성할 수 있다") {
            // given
            val snowflakeId = 1234567890123456789L

            // when
            val author = Author(snowflakeId)

            // then
            author.id shouldBe snowflakeId
        }

        test("일반적인 사용자 ID로 Author를 생성할 수 있다") {
            // given
            val userId = 12345L

            // when
            val author = Author(userId)

            // then
            author.id shouldBe userId
        }
    }

    context("Author 생성 실패") {
        test("0으로 Author를 생성하면 예외가 발생한다") {
            // given
            val zeroId = 0L

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author(zeroId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("음수 값으로 Author를 생성하면 예외가 발생한다") {
            // given
            val negativeId = -1L

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author(negativeId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("큰 음수 값으로 Author를 생성하면 예외가 발생한다") {
            // given
            val largeNegativeId = Long.MIN_VALUE

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author(largeNegativeId)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("여러 음수 값으로 Author 생성이 실패한다") {
            // given
            val negativeIds = listOf(-100L, -999L, -1234567890L)

            // when & then
            negativeIds.forEach { id ->
                val exception =
                    shouldThrow<DocumentDomainException> {
                        Author(id)
                    }
                exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            }
        }
    }

    context("from 메서드") {
        test("유효한 문자열로 Author를 생성할 수 있다") {
            // given
            val validIdString = "123"

            // when
            val author = Author.from(validIdString)

            // then
            author.id shouldBe 123L
        }

        test("큰 숫자 문자열로 Author를 생성할 수 있다") {
            // given
            val largeIdString = "9223372036854775807"

            // when
            val author = Author.from(largeIdString)

            // then
            author.id shouldBe 9223372036854775807L
        }

        test("Snowflake ID 문자열로 Author를 생성할 수 있다") {
            // given
            val snowflakeIdString = "1234567890123456789"

            // when
            val author = Author.from(snowflakeIdString)

            // then
            author.id shouldBe 1234567890123456789L
        }

        test("숫자가 아닌 문자열로 생성하면 예외가 발생한다") {
            // given
            val invalidString = "abc"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(invalidString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe invalidString
            exception.cause shouldNotBe null
        }

        test("빈 문자열로 생성하면 예외가 발생한다") {
            // given
            val emptyString = ""

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(emptyString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe emptyString
            exception.cause shouldNotBe null
        }

        test("공백 문자열로 생성하면 예외가 발생한다") {
            // given
            val blankString = "   "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(blankString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe blankString
            exception.cause shouldNotBe null
        }

        test("특수문자가 포함된 문자열로 생성하면 예외가 발생한다") {
            // given
            val specialCharString = "123@456"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(specialCharString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe specialCharString
            exception.cause shouldNotBe null
        }

        test("소수점이 포함된 문자열로 생성하면 예외가 발생한다") {
            // given
            val decimalString = "123.456"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(decimalString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe decimalString
            exception.cause shouldNotBe null
        }

        test("0 문자열로 생성하면 예외가 발생한다") {
            // given
            val zeroString = "0"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(zeroString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("음수 문자열로 생성하면 예외가 발생한다") {
            // given
            val negativeString = "-123"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(negativeString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe "documentId"
        }

        test("Long 범위를 초과하는 문자열로 생성하면 예외가 발생한다") {
            // given
            val overflowString = "9223372036854775808" // Long.MAX_VALUE + 1

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(overflowString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe overflowString
            exception.cause shouldNotBe null
        }

        test("문자와 숫자가 섞인 문자열로 생성하면 예외가 발생한다") {
            // given
            val mixedString = "123abc456"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Author.from(mixedString)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT
            exception.params[0] shouldBe mixedString
            exception.cause shouldNotBe null
        }
    }

    context("equals와 hashCode") {
        test("같은 값을 가진 Author 객체는 동등하다") {
            // given
            val author1 = Author(123L)
            val author2 = Author(123L)

            // when & then
            author1 shouldBe author2
            author1.hashCode() shouldBe author2.hashCode()
        }

        test("다른 값을 가진 Author 객체는 동등하지 않다") {
            // given
            val author1 = Author(123L)
            val author2 = Author(456L)

            // when & then
            author1 shouldNotBe author2
            author1.hashCode() shouldNotBe author2.hashCode()
        }

        test("같은 객체 참조는 동등하다") {
            // given
            val author = Author(123L)

            // when & then
            author shouldBe author
            author.hashCode() shouldBe author.hashCode()
        }

        test("null과 비교하면 동등하지 않다") {
            // given
            val author = Author(123L)

            // when & then
            (author.equals(null)) shouldBe false
        }

        test("다른 타입의 객체와 비교하면 동등하지 않다") {
            // given
            val author = Author(123L)
            val otherObject = 123L

            // when & then
            (author.equals(otherObject)) shouldBe false
        }

        test("문자열 객체와 비교하면 동등하지 않다") {
            // given
            val author = Author(123L)
            val stringObject = "123"

            // when & then
            (author.equals(stringObject)) shouldBe false
        }

        test("DocumentId 객체와 비교하면 동등하지 않다") {
            // given
            val author = Author(123L)
            val documentId = DocumentId(123L)

            // when & then
            (author.equals(documentId)) shouldBe false
        }
    }

    context("toString") {
        test("Author 객체의 문자열 표현을 반환한다") {
            // given
            val idValue = 123L
            val author = Author(idValue)

            // when
            val result = author.toString()

            // then
            result shouldBe "123"
        }

        test("큰 값의 Author 객체의 문자열 표현을 반환한다") {
            // given
            val largeId = 9223372036854775807L
            val author = Author(largeId)

            // when
            val result = author.toString()

            // then
            result shouldBe "9223372036854775807"
        }

        test("Snowflake ID의 Author 객체의 문자열 표현을 반환한다") {
            // given
            val snowflakeId = 1234567890123456789L
            val author = Author(snowflakeId)

            // when
            val result = author.toString()

            // then
            result shouldBe "1234567890123456789"
        }
    }

    context("경계값 테스트") {
        test("1로 Author를 생성할 수 있다 (최소 유효값)") {
            // given
            val minValidId = 1L

            // when
            val author = Author(minValidId)

            // then
            author.id shouldBe minValidId
        }

        test("Long.MAX_VALUE로 Author를 생성할 수 있다") {
            // given
            val maxId = Long.MAX_VALUE

            // when
            val author = Author(maxId)

            // then
            author.id shouldBe maxId
        }

        test("문자열 '1'로 Author를 생성할 수 있다") {
            // given
            val minValidString = "1"

            // when
            val author = Author.from(minValidString)

            // then
            author.id shouldBe 1L
        }

        test("다양한 경계값으로 Author 생성을 테스트한다") {
            // given
            val boundaryValues =
                listOf(
                    1L to true,
                    0L to false,
                    -1L to false,
                    Long.MAX_VALUE to true,
                    Long.MIN_VALUE to false,
                    100L to true,
                    999999999999999999L to true,
                )

            // when & then
            boundaryValues.forEach { (value, shouldSucceed) ->
                if (shouldSucceed) {
                    val author = Author(value)
                    author.id shouldBe value
                } else {
                    shouldThrow<DocumentDomainException> {
                        Author(value)
                    }
                }
            }
        }
    }

    context("통합 시나리오 테스트") {
        test("from 메서드와 생성자가 동일한 결과를 반환한다") {
            // given
            val idValue = 12345L
            val idString = "12345"

            // when
            val authorFromConstructor = Author(idValue)
            val authorFromString = Author.from(idString)

            // then
            authorFromConstructor shouldBe authorFromString
            authorFromConstructor.hashCode() shouldBe authorFromString.hashCode()
            authorFromConstructor.toString() shouldBe authorFromString.toString()
        }

        test("여러 Author 객체를 Set에 저장할 수 있다") {
            // given
            val author1 = Author(1L)
            val author2 = Author(2L)
            val author3 = Author(1L) // author1과 같은 값

            // when
            val authorSet = setOf(author1, author2, author3)

            // then
            authorSet.size shouldBe 2
            authorSet.contains(author1) shouldBe true
            authorSet.contains(author2) shouldBe true
        }

        test("Author를 Map의 키로 사용할 수 있다") {
            // given
            val author1 = Author(1L)
            val author2 = Author(2L)
            val author3 = Author(1L) // author1과 같은 값

            // when
            val authorMap = mutableMapOf<Author, String>()
            authorMap[author1] = "User 1"
            authorMap[author2] = "User 2"
            authorMap[author3] = "User 1 Updated"

            // then
            authorMap.size shouldBe 2
            authorMap[author1] shouldBe "User 1 Updated"
            authorMap[author2] shouldBe "User 2"
        }

        test("Author 리스트를 정렬할 수 있다") {
            // given
            val authors =
                listOf(
                    Author(5L),
                    Author(1L),
                    Author(3L),
                    Author(2L),
                    Author(4L),
                )

            // when
            val sortedAuthors = authors.sortedBy { it.id }

            // then
            sortedAuthors[0].id shouldBe 1L
            sortedAuthors[1].id shouldBe 2L
            sortedAuthors[2].id shouldBe 3L
            sortedAuthors[3].id shouldBe 4L
            sortedAuthors[4].id shouldBe 5L
        }

        test("Author 컬렉션에서 distinct 연산이 올바르게 동작한다") {
            // given
            val authors =
                listOf(
                    Author(1L),
                    Author(2L),
                    Author(1L),
                    Author(3L),
                    Author(2L),
                    Author(1L),
                )

            // when
            val distinctAuthors = authors.distinct()

            // then
            distinctAuthors.size shouldBe 3
            distinctAuthors.map { it.id }.sorted() shouldBe listOf(1L, 2L, 3L)
        }
    }

    context("값 객체 특성 테스트") {
        test("Author는 불변 객체이다") {
            // given
            val author = Author(123L)
            val initialId = author.id

            // when
            // authorId는 val로 선언되어 있어 수정 불가능

            // then
            author.id shouldBe initialId
        }

        test("동일한 값으로 생성된 Author 객체들은 항상 동등하다") {
            // given
            val id = 42L

            // when
            val author1 = Author(id)
            val author2 = Author(id)
            val author3 = Author.from(id.toString())

            // then
            author1 shouldBe author2
            author2 shouldBe author3
            author1 shouldBe author3
        }
    }
})
