package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.port.UserRepository

class UserReader(
    private val repository: UserRepository,
) {
    fun findByEmail(email: Email): DomainResult<User> =
        repository.findByEmail(email)
            ?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure.NotFound("User", email.value)
}
