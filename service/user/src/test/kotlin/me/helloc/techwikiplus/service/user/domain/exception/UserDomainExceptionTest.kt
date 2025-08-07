package me.helloc.techwikiplus.service.user.domain.exception

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UserDomainExceptionTest : DescribeSpec({

    describe("DomainException") {

        context("상속 관계") {
            it("RuntimeException을 상속한다") {
                val exception = DomainException(ErrorCode.USER_NOT_FOUND)
                exception.shouldBeInstanceOf<RuntimeException>()
            }
        }

        context("ErrorCode 포함") {
            it("ErrorCode를 포함한다") {
                val exception = DomainException(ErrorCode.USER_NOT_FOUND)

                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
                exception.message shouldBe "USER_NOT_FOUND"
                exception.params shouldHaveSize 0
                exception.cause shouldBe null
            }
        }

        context("파라미터 처리") {
            it("파라미터를 포함할 수 있다") {
                val params = arrayOf("test@example.com", "additional info")
                val exception = DomainException(ErrorCode.USER_NOT_FOUND, params)

                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
                exception.params shouldHaveSize 2
                exception.params[0] shouldBe "test@example.com"
                exception.params[1] shouldBe "additional info"
            }
        }

        context("원인 예외") {
            it("원인 예외를 포함할 수 있다") {
                val cause = IllegalArgumentException("root cause")
                val exception = DomainException(ErrorCode.INTERNAL_ERROR, emptyArray(), cause)

                exception.errorCode shouldBe ErrorCode.INTERNAL_ERROR
                exception.cause shouldBe cause
            }
        }
    }

    describe("ErrorCode enum") {

        context("모든 에러 타입 포함") {
            it("User Status 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.USER_DORMANT
                ErrorCode.entries shouldContain ErrorCode.USER_BANNED
                ErrorCode.entries shouldContain ErrorCode.USER_PENDING
                ErrorCode.entries shouldContain ErrorCode.USER_DELETED
            }

            it("User Management 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.DUPLICATE_EMAIL
                ErrorCode.entries shouldContain ErrorCode.DUPLICATE_NICKNAME
                ErrorCode.entries shouldContain ErrorCode.USER_NOT_FOUND
                ErrorCode.entries shouldContain ErrorCode.PENDING_USER_NOT_FOUND
            }

            it("Authentication 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.INVALID_CREDENTIALS
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_MISMATCH
            }

            it("Token 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.INVALID_TOKEN
                ErrorCode.entries shouldContain ErrorCode.TOKEN_EXPIRED
                ErrorCode.entries shouldContain ErrorCode.INVALID_TOKEN_TYPE
            }

            it("Verification 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.INVALID_VERIFICATION_CODE
                ErrorCode.entries shouldContain ErrorCode.REGISTRATION_EXPIRED
                ErrorCode.entries shouldContain ErrorCode.CODE_MISMATCH
            }

            it("Notification 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.NOTIFICATION_FAILED
            }

            it("Application Level 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.SIGNUP_FAILED
                ErrorCode.entries shouldContain ErrorCode.LOGIN_FAILED
                ErrorCode.entries shouldContain ErrorCode.VERIFICATION_FAILED
            }

            it("Email Validation 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.BLANK_EMAIL
                ErrorCode.entries shouldContain ErrorCode.INVALID_EMAIL_FORMAT
            }

            it("Nickname Validation 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.BLANK_NICKNAME
                ErrorCode.entries shouldContain ErrorCode.NICKNAME_TOO_SHORT
                ErrorCode.entries shouldContain ErrorCode.NICKNAME_TOO_LONG
                ErrorCode.entries shouldContain ErrorCode.NICKNAME_CONTAINS_SPACE
                ErrorCode.entries shouldContain ErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR
            }

            it("Password Validation 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.BLANK_PASSWORD
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_TOO_SHORT
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_TOO_LONG
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_NO_UPPERCASE
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_NO_LOWERCASE
                ErrorCode.entries shouldContain ErrorCode.PASSWORD_NO_SPECIAL_CHAR
            }

            it("UserId Validation 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.BLANK_USER_ID
                ErrorCode.entries shouldContain ErrorCode.USER_ID_TOO_LONG
            }

            it("Generic 에러 코드를 포함한다") {
                ErrorCode.entries shouldContain ErrorCode.VALIDATION_ERROR
                ErrorCode.entries shouldContain ErrorCode.DOMAIN_ERROR
                ErrorCode.entries shouldContain ErrorCode.INTERNAL_ERROR
            }
        }
    }

    describe("Validation ErrorCode 파라미터") {

        context("각 검증 타입별 파라미터 처리") {
            it("Email validation 파라미터가 올바르게 처리된다") {
                val emailException = DomainException(ErrorCode.BLANK_EMAIL, arrayOf("email"))
                emailException.params[0] shouldBe "email"
            }

            it("Nickname validation 파라미터가 길이와 함께 처리된다") {
                val nicknameException = DomainException(ErrorCode.NICKNAME_TOO_SHORT, arrayOf<Any>("nickname", 2))
                nicknameException.params[0] shouldBe "nickname"
                nicknameException.params[1] shouldBe 2
            }

            it("Password validation 파라미터가 길이와 함께 처리된다") {
                val passwordException = DomainException(ErrorCode.PASSWORD_TOO_LONG, arrayOf<Any>("password", 30))
                passwordException.params[0] shouldBe "password"
                passwordException.params[1] shouldBe 30
            }

            it("UserId validation 파라미터가 제한값과 함께 처리된다") {
                val userIdException = DomainException(ErrorCode.USER_ID_TOO_LONG, arrayOf<Any>("userId", 64))
                userIdException.params[0] shouldBe "userId"
                userIdException.params[1] shouldBe 64
            }
        }
    }
})
