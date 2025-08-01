package me.helloc.techwikiplus.service.user.domain.model.result

sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()

    sealed class Failure : DomainResult<Nothing>() {
        data class ValidationError(val message: String) : Failure()

        data class BusinessRuleViolation(val rule: String) : Failure()

        data class NotFound(val entity: String, val id: String) : Failure()

        data class Unauthorized(val reason: String) : Failure()

        data class SystemError(val message: String) : Failure()
    }

    inline fun <R> map(transform: (T) -> R): DomainResult<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Failure -> this
        }

    inline fun <R> flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> =
        when (this) {
            is Success -> transform(data)
            is Failure -> this
        }

    inline fun onSuccess(action: (T) -> Unit): DomainResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Failure) -> Unit): DomainResult<T> {
        if (this is Failure) action(this)
        return this
    }

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Failure -> null
        }

    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is Failure -> throw IllegalStateException("Operation failed: $this")
        }
}
