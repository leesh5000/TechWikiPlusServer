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
                val exception = UserDomainException(UserErrorCode.USER_NOT_FOUND)
                exception.shouldBeInstanceOf<RuntimeException>()
            }
        }

        context("ErrorCode 포함") {
            it("ErrorCode를 포함한다") {
                val exception = UserDomainException(UserErrorCode.USER_NOT_FOUND)

                exception.userErrorCode shouldBe UserErrorCode.USER_NOT_FOUND
                exception.message shouldBe "USER_NOT_FOUND"
                exception.params shouldHaveSize 0
                exception.cause shouldBe null
            }
        }

        context("파라미터 처리") {
            it("파라미터를 포함할 수 있다") {
                val params = arrayOf("test@example.com", "additional info")
                val exception = UserDomainException(UserErrorCode.USER_NOT_FOUND, params)

                exception.userErrorCode shouldBe UserErrorCode.USER_NOT_FOUND
                exception.params shouldHaveSize 2
                exception.params[0] shouldBe "test@example.com"
                exception.params[1] shouldBe "additional info"
            }
        }

        context("원인 예외") {
            it("원인 예외를 포함할 수 있다") {
                val cause = IllegalArgumentException("root cause")
                val exception = UserDomainException(UserErrorCode.INTERNAL_ERROR, emptyArray(), cause)

                exception.userErrorCode shouldBe UserErrorCode.INTERNAL_ERROR
                exception.cause shouldBe cause
            }
        }
    }

    describe("ErrorCode enum") {

        context("모든 에러 타입 포함") {
            it("User Status 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.USER_DORMANT
                UserErrorCode.entries shouldContain UserErrorCode.USER_BANNED
                UserErrorCode.entries shouldContain UserErrorCode.USER_PENDING
                UserErrorCode.entries shouldContain UserErrorCode.USER_DELETED
            }

            it("User Management 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.DUPLICATE_EMAIL
                UserErrorCode.entries shouldContain UserErrorCode.DUPLICATE_NICKNAME
                UserErrorCode.entries shouldContain UserErrorCode.USER_NOT_FOUND
                UserErrorCode.entries shouldContain UserErrorCode.NO_STATUS_USER
            }

            it("Authentication 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.INVALID_CREDENTIALS
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_MISMATCH
            }

            it("Token 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.INVALID_TOKEN
                UserErrorCode.entries shouldContain UserErrorCode.TOKEN_EXPIRED
                UserErrorCode.entries shouldContain UserErrorCode.INVALID_TOKEN_TYPE
            }

            it("Verification 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.INVALID_VERIFICATION_CODE
                UserErrorCode.entries shouldContain UserErrorCode.REGISTRATION_EXPIRED
                UserErrorCode.entries shouldContain UserErrorCode.CODE_MISMATCH
            }

            it("Notification 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.NOTIFICATION_FAILED
            }

            it("Application Level 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.SIGNUP_FAILED
                UserErrorCode.entries shouldContain UserErrorCode.LOGIN_FAILED
                UserErrorCode.entries shouldContain UserErrorCode.VERIFICATION_FAILED
            }

            it("Email Validation 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.BLANK_EMAIL
                UserErrorCode.entries shouldContain UserErrorCode.INVALID_EMAIL_FORMAT
            }

            it("Nickname Validation 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.BLANK_NICKNAME
                UserErrorCode.entries shouldContain UserErrorCode.NICKNAME_TOO_SHORT
                UserErrorCode.entries shouldContain UserErrorCode.NICKNAME_TOO_LONG
                UserErrorCode.entries shouldContain UserErrorCode.NICKNAME_CONTAINS_SPACE
                UserErrorCode.entries shouldContain UserErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR
            }

            it("Password Validation 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.BLANK_PASSWORD
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_TOO_SHORT
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_TOO_LONG
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_NO_UPPERCASE
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_NO_LOWERCASE
                UserErrorCode.entries shouldContain UserErrorCode.PASSWORD_NO_SPECIAL_CHAR
            }

            it("UserId Validation 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.BLANK_USER_ID
                UserErrorCode.entries shouldContain UserErrorCode.USER_ID_TOO_LONG
            }

            it("Generic 에러 코드를 포함한다") {
                UserErrorCode.entries shouldContain UserErrorCode.VALIDATION_ERROR
                UserErrorCode.entries shouldContain UserErrorCode.DOMAIN_ERROR
                UserErrorCode.entries shouldContain UserErrorCode.INTERNAL_ERROR
            }
        }
    }

    describe("Validation ErrorCode 파라미터") {

        context("각 검증 타입별 파라미터 처리") {
            it("Email validation 파라미터가 올바르게 처리된다") {
                val emailException = UserDomainException(UserErrorCode.BLANK_EMAIL, arrayOf("email"))
                emailException.params[0] shouldBe "email"
            }

            it("Nickname validation 파라미터가 길이와 함께 처리된다") {
                val nicknameException =
                    UserDomainException(UserErrorCode.NICKNAME_TOO_SHORT, arrayOf<Any>("nickname", 2))
                nicknameException.params[0] shouldBe "nickname"
                nicknameException.params[1] shouldBe 2
            }

            it("Password validation 파라미터가 길이와 함께 처리된다") {
                val passwordException =
                    UserDomainException(UserErrorCode.PASSWORD_TOO_LONG, arrayOf<Any>("password", 30))
                passwordException.params[0] shouldBe "password"
                passwordException.params[1] shouldBe 30
            }

            it("UserId validation 파라미터가 제한값과 함께 처리된다") {
                val userIdException = UserDomainException(UserErrorCode.USER_ID_TOO_LONG, arrayOf<Any>("userId", 64))
                userIdException.params[0] shouldBe "userId"
                userIdException.params[1] shouldBe 64
            }
        }
    }
})
