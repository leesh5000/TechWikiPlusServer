package me.helloc.techwikiplus.user.application.exception

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import me.helloc.techwikiplus.user.application.ApplicationExceptionHandler
import me.helloc.techwikiplus.user.application.CompensationFailedException
import me.helloc.techwikiplus.user.application.RetryExhaustedException
import me.helloc.techwikiplus.user.infrastructure.exception.InfrastructureException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.slf4j.Logger

class ApplicationExceptionHandlerTest : FunSpec({

    lateinit var logger: Logger
    lateinit var handler: ApplicationExceptionHandler

    beforeEach {
        logger = mockk(relaxed = true)
        handler = ApplicationExceptionHandler(logger)
    }

    context("UseCase 실행 처리") {
        test("정상적인 UseCase 실행 시 결과를 반환한다") {
            // given
            val expectedResult = "success"
            val useCase = { expectedResult }

            // when
            val result = handler.execute("TestUseCase", useCase)

            // then
            result shouldBe expectedResult
        }

        test("UseCase 실행 중 예외 발생 시 로깅하고 예외를 전파한다") {
            // given
            val exception = RuntimeException("Test error")
            val useCase = { throw exception }

            // when & then
            val thrown =
                shouldThrow<RuntimeException> {
                    handler.execute("TestUseCase", useCase)
                }

            thrown shouldBe exception
            verify { logger.error("UseCase 실행 중 예외 발생: TestUseCase", exception) }
        }

        test("재시도 가능한 InfrastructureException 발생 시 적절히 처리한다") {
            // given
            val exception = InfrastructureException("Connection failed", retryable = true)
            val useCase = { throw exception }

            // when & then
            val thrown =
                shouldThrow<InfrastructureException> {
                    handler.execute("TestUseCase", useCase)
                }

            thrown shouldBe exception
            verify { logger.warn("재시도 가능한 인프라 예외 발생: TestUseCase - Connection failed") }
        }

        test("재시도 불가능한 InfrastructureException 발생 시 적절히 처리한다") {
            // given
            val exception = InfrastructureException("Fatal error", retryable = false)
            val useCase = { throw exception }

            // when & then
            val thrown =
                shouldThrow<InfrastructureException> {
                    handler.execute("TestUseCase", useCase)
                }

            thrown shouldBe exception
            verify { logger.error("재시도 불가능한 인프라 예외 발생: TestUseCase", exception) }
        }
    }

    context("보상 트랜잭션 처리") {
        test("메인 작업 성공 후 보상 작업도 성공하면 결과를 반환한다") {
            // given
            val mainResult = "main success"
            val mainAction = { mainResult }
            val compensationAction = { }

            // when
            val result =
                handler.executeWithCompensation(
                    "TestUseCase",
                    mainAction,
                    compensationAction,
                )

            // then
            result shouldBe mainResult
        }

        test("메인 작업 실패 시 보상 작업을 실행하지 않는다") {
            // given
            val exception = RuntimeException("Main failed")
            val mainAction = { throw exception }
            var compensationExecuted = false
            val compensationAction = { compensationExecuted = true }

            // when & then
            shouldThrow<RuntimeException> {
                handler.executeWithCompensation(
                    "TestUseCase",
                    mainAction,
                    compensationAction,
                )
            }

            compensationExecuted shouldBe false
        }

        test("메인 작업 성공 후 보상 작업 실패 시 적절한 예외를 발생시킨다") {
            // given
            val mainResult = "main success"
            val mainAction = { mainResult }
            val compensationException = MailDeliveryException("test@example.com")
            val compensationAction = { throw compensationException }

            // when & then
            val thrown =
                shouldThrow<CompensationFailedException> {
                    handler.executeWithCompensation(
                        "TestUseCase",
                        mainAction,
                        compensationAction,
                    )
                }

            thrown.mainResult shouldBe mainResult
            thrown.cause shouldBe compensationException
            verify {
                logger.error(
                    "보상 트랜잭션 실패: TestUseCase - 메인 작업은 성공했으나 후속 작업 실패",
                    compensationException,
                )
            }
        }
    }

    context("재시도 로직") {
        test("재시도 가능한 예외 발생 시 지정된 횟수만큼 재시도한다") {
            // given
            var attemptCount = 0
            val action = {
                attemptCount++
                if (attemptCount < 3) {
                    throw InfrastructureException("Temporary failure", retryable = true)
                }
                "success"
            }

            // when
            val result =
                handler.executeWithRetry(
                    "TestUseCase",
                    action,
                    maxAttempts = 3,
                    delayMillis = 10,
                )

            // then
            result shouldBe "success"
            attemptCount shouldBe 3
            verify(exactly = 2) {
                logger.info(match { it.contains("재시도") })
            }
        }

        test("최대 재시도 횟수 초과 시 예외를 발생시킨다") {
            // given
            val exception = InfrastructureException("Persistent failure", retryable = true)
            val action = { throw exception }

            // when & then
            val thrown =
                shouldThrow<RetryExhaustedException> {
                    handler.executeWithRetry(
                        "TestUseCase",
                        action,
                        maxAttempts = 3,
                        delayMillis = 10,
                    )
                }

            thrown.attempts shouldBe 3
            thrown.cause shouldBe exception
        }

        test("재시도 불가능한 예외는 즉시 전파한다") {
            // given
            val exception = InfrastructureException("Fatal error", retryable = false)
            var attemptCount = 0
            val action = {
                attemptCount++
                throw exception
            }

            // when & then
            shouldThrow<InfrastructureException> {
                handler.executeWithRetry(
                    "TestUseCase",
                    action,
                    maxAttempts = 3,
                )
            }

            attemptCount shouldBe 1
        }
    }
})

// 보상 트랜잭션 실패 예외
class CompensationFailedException(
    val mainResult: Any?,
    cause: Throwable,
) : RuntimeException("보상 트랜잭션 실패", cause)

// 재시도 횟수 초과 예외
class RetryExhaustedException(
    val attempts: Int,
    cause: Throwable,
) : RuntimeException("최대 재시도 횟수($attempts) 초과", cause)
