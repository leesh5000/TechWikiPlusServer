package me.helloc.techwikiplus.service.document.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldStartWith
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class ContentTest : FunSpec({

    context("Content 생성") {
        test("유효한 내용으로 Content를 생성할 수 있다") {
            // given
            val validContent = "Spring Boot는 스프링 기반 애플리케이션을 쉽게 만들 수 있도록 도와주는 프레임워크입니다."

            // when
            val content = Content(validContent)

            // then
            content.value shouldBe validContent
        }

        test("앞뒤 공백이 있는 내용은 자동으로 trim 처리된다") {
            // given
            val contentWithSpaces = "  Spring Boot 가이드  "
            val expectedContent = "Spring Boot 가이드"

            // when
            val content = Content(contentWithSpaces)

            // then
            content.value shouldBe expectedContent
        }

        test("한글 내용을 생성할 수 있다") {
            // given
            val koreanContent = "스프링 부트는 마이크로서비스 아키텍처를 구현하는데 적합한 프레임워크입니다."

            // when
            val content = Content(koreanContent)

            // then
            content.value shouldBe koreanContent
        }

        test("영문 내용을 생성할 수 있다") {
            // given
            val englishContent =
                "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications."

            // when
            val content = Content(englishContent)

            // then
            content.value shouldBe englishContent
        }

        test("숫자가 포함된 내용을 생성할 수 있다") {
            // given
            val contentWithNumbers = "Spring Boot 3.0은 Java 17 이상을 요구합니다."

            // when
            val content = Content(contentWithNumbers)

            // then
            content.value shouldBe contentWithNumbers
        }

        test("HTML 태그가 포함된 내용을 생성할 수 있다") {
            // given
            val htmlContent = "<h1>Spring Boot</h1><p>스프링 부트 가이드입니다.</p>"

            // when
            val content = Content(htmlContent)

            // then
            content.value shouldBe htmlContent
        }

        test("마크다운 문법이 포함된 내용을 생성할 수 있다") {
            // given
            val markdownContent =
                """
                # Spring Boot Guide
                ## Getting Started
                - Step 1: Install Java
                - Step 2: Install Spring Boot CLI
                ```java
                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                ```
                """.trimIndent()

            // when
            val content = Content(markdownContent)

            // then
            content.value shouldBe markdownContent
        }

        test("특수문자가 포함된 내용을 생성할 수 있다") {
            // given
            val specialCharsContent =
                """
                @Controller @Service @Repository
                ${"`"}${"`"}${"`"}bash
                $ ./gradlew bootRun
                ${"`"}${"`"}${"`"}
                http://localhost:8080/api/v1/users/{id}
                <dependency>spring-boot-starter-web</dependency>
                #include <stdio.h> // C header
                SELECT * FROM users WHERE age >= 18;
                const sum = (a, b) => a + b;
                10% discount! $99.99 -> $89.99
                """.trimIndent()

            // when
            val content = Content(specialCharsContent)

            // then
            content.value shouldBe specialCharsContent
        }

        test("이모지가 포함된 내용을 생성할 수 있다") {
            // given
            val emojiContent = "Spring Boot 🚀 시작하기! 🎉 재미있는 프로그래밍 😊"

            // when
            val content = Content(emojiContent)

            // then
            content.value shouldBe emojiContent
        }

        test("개행 문자가 포함된 내용을 생성할 수 있다") {
            // given
            val multilineContent =
                """
                첫 번째 줄
                두 번째 줄
                
                네 번째 줄 (빈 줄 다음)
                """.trimIndent()

            // when
            val content = Content(multilineContent)

            // then
            content.value shouldBe multilineContent
        }

        test("최소 길이(1자)의 내용을 생성할 수 있다") {
            // given
            val minLengthContent = "A"

            // when
            val content = Content(minLengthContent)

            // then
            content.value shouldBe minLengthContent
            content.value shouldHaveLength 1
        }

        test("최대 길이(50000자)의 내용을 생성할 수 있다") {
            // given
            val maxLengthContent = "A".repeat(50000)

            // when
            val content = Content(maxLengthContent)

            // then
            content.value shouldBe maxLengthContent
            content.value shouldHaveLength 50000
        }

        test("코드 블록이 포함된 기술 문서 내용을 생성할 수 있다") {
            // given
            val technicalContent =
                """
                ## Spring Boot Application Properties
                
                application.yml:
                ```yaml
                spring:
                  datasource:
                    url: jdbc:mysql://localhost:3306/mydb
                    username: root
                    password: ${"`"}${"$"}{DB_PASSWORD}${"`"}
                  jpa:
                    hibernate:
                      ddl-auto: update
                    show-sql: true
                ```
                
                application.properties:
                ```properties
                spring.datasource.url=jdbc:mysql://localhost:3306/mydb
                spring.datasource.username=root
                spring.datasource.password=${"$"}{DB_PASSWORD}
                ```
                """.trimIndent()

            // when
            val content = Content(technicalContent)

            // then
            content.value shouldBe technicalContent
        }
    }

    context("Content 생성 실패") {
        test("빈 문자열로 Content를 생성하면 예외가 발생한다") {
            // given
            val emptyContent = ""

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(emptyContent)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_CONTENT
        }

        test("공백만 있는 문자열로 Content를 생성하면 예외가 발생한다") {
            // given
            val blankContent = "   "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(blankContent)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_CONTENT
        }

        test("탭과 개행만 있는 문자열로 Content를 생성하면 예외가 발생한다") {
            // given
            val whitespaceContent = "\t\n\r\n\t"

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(whitespaceContent)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_CONTENT
        }

        test("최대 길이(50000자)를 초과하는 내용으로 생성하면 예외가 발생한다") {
            // given
            val tooLongContent = "A".repeat(50001)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(tooLongContent)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.CONTENT_TOO_LONG
        }
    }

    context("equals와 hashCode") {
        test("같은 값을 가진 Content 객체는 동등하다") {
            // given
            val content1 = Content("Spring Boot Guide")
            val content2 = Content("Spring Boot Guide")

            // when & then
            content1 shouldBe content2
            content1.hashCode() shouldBe content2.hashCode()
        }

        test("다른 값을 가진 Content 객체는 동등하지 않다") {
            // given
            val content1 = Content("Spring Boot Guide")
            val content2 = Content("Spring Framework Guide")

            // when & then
            content1 shouldNotBe content2
            content1.hashCode() shouldNotBe content2.hashCode()
        }

        test("trim 처리 후 같은 값을 가진 Content 객체는 동등하다") {
            // given
            val content1 = Content("  Spring Boot Guide  ")
            val content2 = Content("Spring Boot Guide")

            // when & then
            content1 shouldBe content2
            content1.hashCode() shouldBe content2.hashCode()
        }

        test("같은 객체 참조는 동등하다") {
            // given
            val content = Content("Spring Boot Guide")

            // when & then
            content shouldBe content
        }

        test("null과 비교하면 동등하지 않다") {
            // given
            val content = Content("Spring Boot Guide")

            // when & then
            (content.equals(null)) shouldBe false
        }

        test("다른 타입의 객체와 비교하면 동등하지 않다") {
            // given
            val content = Content("Spring Boot Guide")
            val otherObject = "Spring Boot Guide"

            // when & then
            (content.equals(otherObject)) shouldBe false
        }
    }

    context("toString") {
        test("짧은 Content 객체의 문자열 표현을 반환한다") {
            // given
            val shortContent = "Spring Boot"
            val content = Content(shortContent)

            // when
            val result = content.toString()

            // then
            result shouldBe "Content(value=$shortContent)"
        }

        test("50자를 초과하는 Content 객체는 축약된 문자열 표현을 반환한다") {
            // given
            val longContentValue = "A".repeat(100)
            val content = Content(longContentValue)

            // when
            val result = content.toString()

            // then
            result shouldStartWith "Content(value="
            result shouldContain "..."
            result shouldBe "Content(value=${"A".repeat(50)}...)"
        }

        test("정확히 50자인 Content 객체는 전체 문자열을 표시한다") {
            // given
            val exactContent = "A".repeat(50)
            val content = Content(exactContent)

            // when
            val result = content.toString()

            // then
            result shouldBe "Content(value=$exactContent)"
        }
    }

    context("경계값 테스트") {
        test("정확히 50000자의 내용을 생성할 수 있다") {
            // given
            val exactMaxContent = "A".repeat(50000)

            // when
            val content = Content(exactMaxContent)

            // then
            content.value shouldHaveLength 50000
        }

        test("50001자의 내용으로 생성하면 예외가 발생한다") {
            // given
            val overMaxContent = "A".repeat(50001)

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(overMaxContent)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.CONTENT_TOO_LONG
        }

        test("trim 후 빈 문자열이 되면 예외가 발생한다") {
            // given
            val spacesOnly = "\t \n \r "

            // when & then
            val exception =
                shouldThrow<DocumentDomainException> {
                    Content(spacesOnly)
                }
            exception.documentErrorCode shouldBe DocumentErrorCode.BLANK_CONTENT
        }

        test("단일 문자로 Content를 생성할 수 있다") {
            // given
            val singleChar = "X"

            // when
            val content = Content(singleChar)

            // then
            content.value shouldBe singleChar
            content.value shouldHaveLength 1
        }
    }

    context("복합 시나리오 테스트") {
        test("실제 기술 블로그 포스트 내용을 생성할 수 있다") {
            // given
            val blogPost =
                """
                # Spring Boot와 Docker를 활용한 마이크로서비스 구축
                
                ## 1. 개요
                마이크로서비스 아키텍처(MSA)는 현대 소프트웨어 개발의 핵심 패러다임이 되었습니다.
                
                ## 2. 환경 설정
                ### 2.1 Docker 설치
                ```bash
                $ docker --version
                Docker version 20.10.8, build 3967b7d
                ```
                
                ### 2.2 Spring Boot 프로젝트 생성
                ```java
                @SpringBootApplication
                @EnableEurekaClient
                public class MicroserviceApplication {
                    public static void main(String[] args) {
                        SpringApplication.run(MicroserviceApplication.class, args);
                    }
                }
                ```
                
                ## 3. Dockerfile 작성
                ```dockerfile
                FROM openjdk:17-jdk-slim
                COPY target/*.jar app.jar
                ENTRYPOINT ["java", "-jar", "/app.jar"]
                ```
                
                ## 4. 결론
                Spring Boot와 Docker를 함께 사용하면 확장 가능한 마이크로서비스를 쉽게 구축할 수 있습니다.
                """.trimIndent()

            // when
            val content = Content(blogPost)

            // then
            content.value shouldBe blogPost
        }

        test("다양한 프로그래밍 언어 코드가 포함된 내용을 생성할 수 있다") {
            // given
            val polyglotContent =
                """
                // Java
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                
                # Python
                def hello():
                    print("Hello, World!")
                
                // JavaScript
                const hello = () => console.log("Hello, World!");
                
                /* C++ */
                #include <iostream>
                int main() {
                    std::cout << "Hello, World!" << std::endl;
                    return 0;
                }
                
                -- SQL
                SELECT 'Hello, World!' AS greeting;
                """.trimIndent()

            // when
            val content = Content(polyglotContent)

            // then
            content.value shouldBe polyglotContent
        }

        test("JSON 데이터가 포함된 내용을 생성할 수 있다") {
            // given
            val jsonContent =
                """
                API Response Example:
                {
                    "status": "success",
                    "data": {
                        "id": 1,
                        "name": "Spring Boot",
                        "version": "3.0.0",
                        "features": ["auto-configuration", "embedded-server", "actuator"],
                        "dependencies": {
                            "spring-core": "6.0.0",
                            "spring-web": "6.0.0"
                        }
                    },
                    "timestamp": "2024-01-01T00:00:00Z"
                }
                """.trimIndent()

            // when
            val content = Content(jsonContent)

            // then
            content.value shouldBe jsonContent
        }
    }
})
