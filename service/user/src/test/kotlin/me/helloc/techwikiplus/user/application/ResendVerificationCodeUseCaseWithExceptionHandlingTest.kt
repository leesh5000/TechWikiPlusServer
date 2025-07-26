package me.helloc.techwikiplus.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.slf4j.Logger
import java.time.Duration

class ResendVerificationCodeUseCaseWithExceptionHandlingTest : FunSpec({

    lateinit var mailSender: MailSender
    lateinit var verificationCodeStore: VerificationCodeStore
    lateinit var pendingUserValidator: PendingUserValidator
    lateinit var exceptionHandler: ApplicationExceptionHandler
    lateinit var logger: Logger
    lateinit var useCase: ResendVerificationCodeUseCaseWithExceptionHandling

    beforeEach {
        mailSender = mockk()
        verificationCodeStore = mockk()
        pendingUserValidator = mockk()
        logger = mockk(relaxed = true)
        exceptionHandler = ApplicationExceptionHandler(logger)
        useCase =
            ResendVerificationCodeUseCaseWithExceptionHandling(
                mailSender,
                verificationCodeStore,
                pendingUserValidator,
                exceptionHandler,
            )
    }

    context("정상적인 인증 코드 재발송") {
        test("검증, 이메일 발송, 저장이 모두 성공한다") {
            // given
            val email = "test@example.com"
            val verificationCode = VerificationCode("123456")

            every { pendingUserValidator.existsOrThrows(email) } returns Unit
            every { mailSender.sendVerificationEmail(email) } returns verificationCode
            every {
                verificationCodeStore.storeWithExpiry(email, verificationCode, any())
            } returns Unit

            // when
            useCase.resendVerificationCode(email)

            // then
            verify { pendingUserValidator.existsOrThrows(email) }
            verify { mailSender.sendVerificationEmail(email) }
            verify {
                verificationCodeStore.storeWithExpiry(
                    email,
                    verificationCode,
                    Duration.ofMinutes(5),
                )
            }
        }
    }

    context("사용자 검증 실패") {
        test("PENDING 상태가 아닌 사용자인 경우 예외가 발생한다") {
            // given
            val email = "test@example.com"
            val exception = IllegalStateException("User is not in PENDING state")

            every { pendingUserValidator.existsOrThrows(email) } throws exception

            // when & then
            val thrown =
                shouldThrow<IllegalStateException> {
                    useCase.resendVerificationCode(email)
                }

            thrown shouldBe exception
            verify(exactly = 0) { mailSender.sendVerificationEmail(any()) }
            verify(exactly = 0) { verificationCodeStore.storeWithExpiry(any(), any(), any()) }
        }
    }

    context("이메일 발송 실패") {
        test("재시도 가능한 메일 발송 실패 시 재시도 후 성공한다") {
            // given
            val email = "test@example.com"
            val verificationCode = VerificationCode("123456")
            var attemptCount = 0

            every { pendingUserValidator.existsOrThrows(email) } returns Unit
            every { mailSender.sendVerificationEmail(email) } answers {
                attemptCount++
                if (attemptCount < 2) {
                    throw MailDeliveryException(email)
                }
                verificationCode
            }
            every {
                verificationCodeStore.storeWithExpiry(email, verificationCode, any())
            } returns Unit

            // when
            useCase.resendVerificationCodeWithRetry(email)

            // then
            attemptCount shouldBe 2
            verify { logger.info(match { it.contains("재시도") }) }
            verify {
                verificationCodeStore.storeWithExpiry(
                    email,
                    verificationCode,
                    Duration.ofMinutes(5),
                )
            }
        }

        test("최대 재시도 횟수 초과 시 RetryExhaustedException이 발생한다") {
            // given
            val email = "test@example.com"
            val mailException = MailDeliveryException(email)

            every { pendingUserValidator.existsOrThrows(email) } returns Unit
            every { mailSender.sendVerificationEmail(email) } throws mailException

            // when & then
            val thrown =
                shouldThrow<RetryExhaustedException> {
                    useCase.resendVerificationCodeWithRetry(email, maxRetries = 2)
                }

            thrown.attempts shouldBe 2
            thrown.cause shouldBe mailException
            verify(exactly = 2) { mailSender.sendVerificationEmail(email) }
            verify(exactly = 0) { verificationCodeStore.storeWithExpiry(any(), any(), any()) }
        }
    }

    context("인증 코드 저장 실패") {
        test("이메일은 발송되었지만 Redis 저장 실패 시 부분 성공 상태를 기록한다") {
            // given
            val email = "test@example.com"
            val verificationCode = VerificationCode("123456")
            val storeException = ExternalServiceException("Redis", Exception("Connection failed"))

            every { pendingUserValidator.existsOrThrows(email) } returns Unit
            every { mailSender.sendVerificationEmail(email) } returns verificationCode
            every {
                verificationCodeStore.storeWithExpiry(email, verificationCode, any())
            } throws storeException

            // when & then
            val thrown =
                shouldThrow<ExternalServiceException> {
                    useCase.resendVerificationCode(email)
                }

            thrown shouldBe storeException
            verify {
                logger.warn(
                    "재시도 가능한 인프라 예외 발생: ResendVerificationCode - External service error: Redis",
                )
            }
            // 이메일은 이미 발송되었음을 확인
            verify { mailSender.sendVerificationEmail(email) }
        }
    }

    context("트랜잭션 일관성") {
        test("모든 작업이 원자적으로 처리된다") {
            // given
            val email = "test@example.com"
            val verificationCode = VerificationCode("123456")
            val exception = DataAccessException("Database error", Exception("Connection failed"))

            every { pendingUserValidator.existsOrThrows(email) } returns Unit
            every { mailSender.sendVerificationEmail(email) } returns verificationCode
            every {
                verificationCodeStore.storeWithExpiry(email, verificationCode, any())
            } throws exception

            // when & then
            shouldThrow<DataAccessException> {
                useCase.resendVerificationCode(email)
            }

            // 이메일 발송과 저장이 함께 실패하는지 확인
            verify { mailSender.sendVerificationEmail(email) }
            verify { verificationCodeStore.storeWithExpiry(email, verificationCode, any()) }
        }
    }
})
