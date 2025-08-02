package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.EmailValidationException

class EmailTest : FunSpec({

    test("should create valid email") {
        val email = Email("user@example.com")
        email.value shouldBe "user@example.com"
    }

    test("should reject null or empty email") {
        val exception1 =
            shouldThrow<EmailValidationException> {
                Email("")
            }
        exception1.errorCode shouldBe EmailValidationException.BLANK_EMAIL
        exception1.field shouldBe "email"

        val exception2 =
            shouldThrow<EmailValidationException> {
                Email("   ")
            }
        exception2.errorCode shouldBe EmailValidationException.BLANK_EMAIL
    }

    test("should reject invalid email format - missing @") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("userexample.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
        exception.field shouldBe "email"
    }

    test("should reject invalid email format - missing domain") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("user@")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("should reject invalid email format - missing local part") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("@example.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("should reject invalid email format - invalid characters") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("user name@example.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("should accept various valid email formats") {
        Email("simple@example.com").value shouldBe "simple@example.com"
        Email("very.common@example.com").value shouldBe "very.common@example.com"
        Email("disposable.style.email.with+symbol@example.com").value shouldBe
            "disposable.style.email.with+symbol@example.com"
        Email("other.email-with-hyphen@example.com").value shouldBe "other.email-with-hyphen@example.com"
        Email("x@example.com").value shouldBe "x@example.com"
        Email("user@subdomain.example.com").value shouldBe "user@subdomain.example.com"
    }

    test("should be immutable") {
        val email = Email("user@example.com")
        email.value shouldBe "user@example.com"
        // Value should not be modifiable (enforced by val property)
    }

    test("should implement equals correctly") {
        val email1 = Email("user@example.com")
        val email2 = Email("user@example.com")
        val email3 = Email("another@example.com")

        email1 shouldBe email2
        email1 shouldNotBe email3
        email1 shouldNotBe null
        email1 shouldNotBe "user@example.com"
    }

    test("should implement hashCode correctly") {
        val email1 = Email("user@example.com")
        val email2 = Email("user@example.com")
        val email3 = Email("another@example.com")

        email1.hashCode() shouldBe email2.hashCode()
        email1.hashCode() shouldNotBe email3.hashCode()
    }

    test("should implement toString correctly") {
        val email = Email("user@example.com")
        email.toString() shouldBe "Email(value=user@example.com)"
    }
})
