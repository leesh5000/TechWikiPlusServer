package me.helloc.techwikiplus.service.document.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class TitleTest : FunSpec({

    context("Title 생성") {
        test("유효한 제목으로 Title을 생성할 수 있다") {
            // given
            val validTitle = "Spring Boot 시작하기"

            // when
            val title = Title(validTitle)

            // then
            title.value shouldBe validTitle
        }

        test("앞뒤 공백이 있는 제목은 자동으로 trim 처리된다") {
            // given
            val titleWithSpaces = "  Spring Boot 시작하기  "
            val expectedTitle = "Spring Boot 시작하기"

            // when
            val title = Title(titleWithSpaces)

            // then
            title.value shouldBe expectedTitle
        }

        test("한글 제목을 생성할 수 있다") {
            // given
            val koreanTitle = "스프링 부트 시작하기"

            // when
            val title = Title(koreanTitle)

            // then
            title.value shouldBe koreanTitle
        }

        test("영문 제목을 생성할 수 있다") {
            // given
            val englishTitle = "Getting Started with Spring Boot"

            // when
            val title = Title(englishTitle)

            // then
            title.value shouldBe englishTitle
        }

        test("숫자가 포함된 제목을 생성할 수 있다") {
            // given
            val titleWithNumbers = "Spring Boot 3.0 업데이트"

            // when
            val title = Title(titleWithNumbers)

            // then
            title.value shouldBe titleWithNumbers
        }

        test("허용된 특수문자가 포함된 제목을 생성할 수 있다") {
            // given
            val specialChars =
                listOf(
                    "Spring-Boot 가이드",
                    "Spring_Boot 가이드",
                    "Spring Boot 3.0",
                    "Spring, Boot",
                    "Spring (Boot)",
                    "Spring: Boot",
                    "Spring/Boot",
                    "Spring@Boot",
                    "Spring #1",
                    "Spring & Boot",
                    "Spring + Boot",
                    "[Spring Boot]",
                    "{Spring Boot}",
                    "Spring's Boot",
                    "\"Spring Boot\"",
                )

            // when & then
            specialChars.forEach { titleText ->
                val title = Title(titleText)
                title.value shouldBe titleText
            }
        }

        test("최소 길이(1자)의 제목을 생성할 수 있다") {
            // given
            val minLengthTitle = "A"

            // when
            val title = Title(minLengthTitle)

            // then
            title.value shouldBe minLengthTitle
            title.value shouldHaveLength 1
        }

        test("최대 길이(200자)의 제목을 생성할 수 있다") {
            // given
            val maxLengthTitle = "A".repeat(200)

            // when
            val title = Title(maxLengthTitle)

            // then
            title.value shouldBe maxLengthTitle
            title.value shouldHaveLength 200
        }
    }

    context("Title 생성 실패") {
        test("빈 문자열로 Title을 생성하면 예외가 발생한다") {
            // given
            val emptyTitle = ""

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(emptyTitle)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_TITLE
        }

        test("공백만 있는 문자열로 Title을 생성하면 예외가 발생한다") {
            // given
            val blankTitle = "   "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(blankTitle)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_TITLE
        }

        test("최대 길이(200자)를 초과하는 제목으로 생성하면 예외가 발생한다") {
            // given
            val tooLongTitle = "A".repeat(201)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(tooLongTitle)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.TITLE_TOO_LONG
        }

        test("허용되지 않은 특수문자가 포함된 제목으로 생성하면 예외가 발생한다") {
            // given
            val invalidChars =
                listOf(
                    "Spring<Boot>",
                    "Spring>Boot",
                    "Spring|Boot",
                    "Spring\\Boot",
                    "Spring~Boot",
                    "Spring`Boot",
                    "Spring!Boot",
                    "Spring%Boot",
                    "Spring^Boot",
                    "Spring*Boot",
                    "Spring=Boot",
                    "Spring;Boot",
                    "Spring?Boot",
                )

            // when & then
            invalidChars.forEach { titleText ->
                val exception =
                    shouldThrow<DocumentDomainException> {
                        Title(titleText)
                    }
                exception.documentErrorCode shouldBe DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR
            }
        }

        test("이모지가 포함된 제목으로 생성하면 예외가 발생한다") {
            // given
            val emojiTitle = "Spring Boot 🚀"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(emojiTitle)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR
        }

        test("제어 문자가 포함된 제목으로 생성하면 예외가 발생한다") {
            // given
            val titleWithControlChar = "Spring\nBoot"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(titleWithControlChar)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR
        }
    }

    context("equals와 hashCode") {
        test("같은 값을 가진 Title 객체는 동등하다") {
            // given
            val title1 = Title("Spring Boot")
            val title2 = Title("Spring Boot")

            // when & then
            title1 shouldBe title2
            title1.hashCode() shouldBe title2.hashCode()
        }

        test("다른 값을 가진 Title 객체는 동등하지 않다") {
            // given
            val title1 = Title("Spring Boot")
            val title2 = Title("Spring Framework")

            // when & then
            title1 shouldNotBe title2
            title1.hashCode() shouldNotBe title2.hashCode()
        }

        test("trim 처리 후 같은 값을 가진 Title 객체는 동등하다") {
            // given
            val title1 = Title("  Spring Boot  ")
            val title2 = Title("Spring Boot")

            // when & then
            title1 shouldBe title2
            title1.hashCode() shouldBe title2.hashCode()
        }

        test("같은 객체 참조는 동등하다") {
            // given
            val title = Title("Spring Boot")

            // when & then
            title shouldBe title
        }

        test("null과 비교하면 동등하지 않다") {
            // given
            val title = Title("Spring Boot")

            // when & then
            (title.equals(null)) shouldBe false
        }

        test("다른 타입의 객체와 비교하면 동등하지 않다") {
            // given
            val title = Title("Spring Boot")
            val otherObject = "Spring Boot"

            // when & then
            (title.equals(otherObject)) shouldBe false
        }
    }

    context("toString") {
        test("Title 객체의 문자열 표현을 반환한다") {
            // given
            val titleValue = "Spring Boot"
            val title = Title(titleValue)

            // when
            val result = title.toString()

            // then
            result shouldBe "Title(value=$titleValue)"
        }
    }

    context("경계값 테스트") {
        test("정확히 200자의 제목을 생성할 수 있다") {
            // given
            val exactMaxTitle = "A".repeat(200)

            // when
            val title = Title(exactMaxTitle)

            // then
            title.value shouldHaveLength 200
        }

        test("201자의 제목으로 생성하면 예외가 발생한다") {
            // given
            val overMaxTitle = "A".repeat(201)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(overMaxTitle)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.TITLE_TOO_LONG
        }

        test("trim 후 빈 문자열이 되면 예외가 발생한다") {
            // given
            val spacesOnly = "\t \n \r "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Title(spacesOnly)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_TITLE
        }
    }

    context("복합 시나리오 테스트") {
        test("한글, 영문, 숫자, 특수문자가 모두 포함된 제목을 생성할 수 있다") {
            // given
            val complexTitle = "Spring Boot 3.0 - 한글 가이드 #1 (2024년)"

            // when
            val title = Title(complexTitle)

            // then
            title.value shouldBe complexTitle
        }

        test("공백과 특수문자가 연속으로 있는 제목을 생성할 수 있다") {
            // given
            val consecutiveSpecialTitle = "Spring  Boot--Framework___Guide"

            // when
            val title = Title(consecutiveSpecialTitle)

            // then
            title.value shouldBe consecutiveSpecialTitle
        }
    }
})
