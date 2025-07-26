package me.helloc.techwikiplus.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.slf4j.Logger

class UserSignUpUseCaseWithExceptionHandlingTest : FunSpec({

    lateinit var userRegister: UserRegister
    lateinit var verificationCodeSender: VerificationCodeSender
    lateinit var exceptionHandler: ApplicationExceptionHandler
    lateinit var logger: Logger
    lateinit var useCase: UserSignUpUseCaseWithExceptionHandling

    beforeEach {
        userRegister = mockk()
        verificationCodeSender = mockk()
        logger = mockk(relaxed = true)
        exceptionHandler = ApplicationExceptionHandler(logger)
        useCase =
            UserSignUpUseCaseWithExceptionHandling(
                userRegister,
                verificationCodeSender,
                exceptionHandler,
            )
    }

    context("정상적인 회원가입") {
        test("사용자 등록과 이메일 발송이 모두 성공한다") {
            // given
            val email = "test@example.com"
            val nickname = "testuser"
            val password = "password123"

            every { userRegister.registerPendingUser(email, nickname, password) } returns mockk()
            every { verificationCodeSender.sendMail(email) } returns mockk()

            // when
            useCase.signUp(email, nickname, password)

            // then
            verify { userRegister.registerPendingUser(email, nickname, password) }
            verify { verificationCodeSender.sendMail(email) }
        }
    }

    context("사용자 등록 실패") {
        test("데이터베이스 접근 실패 시 예외가 전파되고 로깅된다") {
            // given
            val email = "test@example.com"
            val nickname = "testuser"
            val password = "password123"
            val exception = DataAccessException("Database connection failed", Exception("Connection error"))

            every { userRegister.registerPendingUser(email, nickname, password) } throws exception

            // when & then
            val thrown =
                shouldThrow<DataAccessException> {
                    useCase.signUp(email, nickname, password)
                }

            thrown shouldBe exception
            verify { logger.error("재시도 불가능한 인프라 예외 발생: UserSignUp", exception) }
            verify(exactly = 0) { verificationCodeSender.sendMail(any()) }
        }
    }

    context("이메일 발송 실패") {
        test("사용자는 등록되었지만 이메일 발송 실패 시 CompensationFailedException이 발생한다") {
            // given
            val email = "test@example.com"
            val nickname = "testuser"
            val password = "password123"
            val mailException = MailDeliveryException(email)

            every { userRegister.registerPendingUser(email, nickname, password) } returns mockk()
            every { verificationCodeSender.sendMail(email) } throws mailException

            // when & then
            val thrown =
                shouldThrow<CompensationFailedException> {
                    useCase.signUp(email, nickname, password)
                }

            thrown.cause shouldBe mailException
            verify { userRegister.registerPendingUser(email, nickname, password) }
            verify { verificationCodeSender.sendMail(email) }
            verify {
                logger.error(
                    "보상 트랜잭션 실패: UserSignUp - 메인 작업은 성공했으나 후속 작업 실패",
                    mailException,
                )
            }
        }
    }

    context("재시도 가능한 이메일 발송") {
        test("일시적인 이메일 발송 실패 시 재시도 후 성공한다") {
            // given
            val email = "test@example.com"
            val nickname = "testuser"
            val password = "password123"
            var attemptCount = 0

            every { userRegister.registerPendingUser(email, nickname, password) } returns mockk()
            every { verificationCodeSender.sendMail(email) } answers {
                attemptCount++
                if (attemptCount < 2) {
                    throw MailDeliveryException(email)
                }
                mockk<VerificationCode>()
            }

            // when
            useCase.signUpWithRetry(email, nickname, password)

            // then
            attemptCount shouldBe 2
            verify { logger.info(match { it.contains("재시도") }) }
        }

        test("최대 재시도 횟수 초과 시 RetryExhaustedException이 발생한다") {
            // given
            val email = "test@example.com"
            val nickname = "testuser"
            val password = "password123"
            val mailException = MailDeliveryException(email)

            every { userRegister.registerPendingUser(email, nickname, password) } returns mockk()
            every { verificationCodeSender.sendMail(email) } throws mailException

            // when & then
            val thrown =
                shouldThrow<CompensationFailedException> {
                    useCase.signUpWithRetry(email, nickname, password, maxRetries = 2)
                }

            thrown.cause.shouldBeInstanceOf<RetryExhaustedException>()
            verify(exactly = 2) { verificationCodeSender.sendMail(email) }
        }
    }
})
