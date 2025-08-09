package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UserProfileFacade(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) : UserProfileUseCase {
    override fun execute(targetUserId: UserId): UserProfileUseCase.Result {
        // Check authorization
        authorizationService.requireUserAccess(targetUserId)

        val user = userReader.get(targetUserId)

        return UserProfileUseCase.Result(
            userId = user.id,
            email = user.email.value,
            nickname = user.nickname.value,
            role = user.role,
            status = user.status,
            createdAt = user.createdAt,
            modifiedAt = user.modifiedAt,
        )
    }
}
