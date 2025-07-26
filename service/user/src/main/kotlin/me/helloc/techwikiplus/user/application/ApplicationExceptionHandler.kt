package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.infrastructure.exception.InfrastructureException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 애플리케이션 계층의 예외 처리를 담당하는 핸들러
 * UseCase 실행 중 발생하는 예외를 일관되게 처리하고 로깅한다
 */
class ApplicationExceptionHandler(
    private val logger: Logger = LoggerFactory.getLogger(ApplicationExceptionHandler::class.java),
) {
    /**
     * UseCase를 실행하고 예외를 처리한다
     *
     * @param useCaseName UseCase 이름 (로깅용)
     * @param action 실행할 UseCase 로직
     * @return UseCase 실행 결과
     * @throws Exception UseCase 실행 중 발생한 예외
     */
    fun <T> execute(
        useCaseName: String,
        action: () -> T,
    ): T {
        return try {
            action()
        } catch (e: InfrastructureException) {
            handleInfrastructureException(useCaseName, e)
            throw e
        } catch (e: Exception) {
            logger.error("UseCase 실행 중 예외 발생: $useCaseName", e)
            throw e
        }
    }

    /**
     * 메인 작업과 보상 작업을 함께 실행한다
     * 메인 작업 성공 후 보상 작업이 실패하면 CompensationFailedException을 발생시킨다
     *
     * @param useCaseName UseCase 이름 (로깅용)
     * @param mainAction 메인 작업
     * @param compensationAction 보상 작업 (메인 작업 성공 후 실행)
     * @return 메인 작업의 결과
     * @throws CompensationFailedException 보상 작업 실패 시
     */
    fun <T> executeWithCompensation(
        useCaseName: String,
        mainAction: () -> T,
        compensationAction: () -> Unit,
    ): T {
        val mainResult = execute(useCaseName, mainAction)

        try {
            compensationAction()
        } catch (e: Exception) {
            logger.error(
                "보상 트랜잭션 실패: $useCaseName - 메인 작업은 성공했으나 후속 작업 실패",
                e,
            )
            throw CompensationFailedException(mainResult, e)
        }

        return mainResult
    }

    /**
     * 재시도 로직을 포함하여 작업을 실행한다
     * 재시도 가능한 InfrastructureException 발생 시 지정된 횟수만큼 재시도한다
     *
     * @param useCaseName UseCase 이름 (로깅용)
     * @param action 실행할 작업
     * @param maxAttempts 최대 시도 횟수
     * @param delayMillis 재시도 간격 (밀리초)
     * @return 작업 실행 결과
     * @throws RetryExhaustedException 최대 재시도 횟수 초과 시
     */
    fun <T> executeWithRetry(
        useCaseName: String,
        action: () -> T,
        maxAttempts: Int = 3,
        delayMillis: Long = 1000,
    ): T {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return action()
            } catch (e: InfrastructureException) {
                if (!e.retryable) {
                    throw e
                }

                lastException = e
                if (attempt < maxAttempts - 1) {
                    logger.info("재시도 중: $useCaseName - 시도 ${attempt + 1}/$maxAttempts")
                    Thread.sleep(delayMillis)
                }
            }
        }

        throw RetryExhaustedException(maxAttempts, lastException!!)
    }

    private fun handleInfrastructureException(
        useCaseName: String,
        e: InfrastructureException,
    ) {
        if (e.retryable) {
            logger.warn("재시도 가능한 인프라 예외 발생: $useCaseName - ${e.message}")
        } else {
            logger.error("재시도 불가능한 인프라 예외 발생: $useCaseName", e)
        }
    }
}

/**
 * 보상 트랜잭션 실패를 나타내는 예외
 * 메인 작업은 성공했으나 후속 작업이 실패한 경우 발생
 */
class CompensationFailedException(
    val mainResult: Any?,
    cause: Throwable,
) : RuntimeException("보상 트랜잭션 실패", cause)

/**
 * 재시도 횟수 초과를 나타내는 예외
 */
class RetryExhaustedException(
    val attempts: Int,
    cause: Throwable,
) : RuntimeException("최대 재시도 횟수($attempts) 초과", cause)
