package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.web.port.GetUserProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class GetUserProfileFacade(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) : GetUserProfileUseCase {
    override fun execute(targetUserId: UserId): GetUserProfileUseCase.Result {
        // Check authorization
        authorizationService.requireUserAccess(targetUserId)

        val user = userReader.getActiveUserBy(targetUserId)

        return GetUserProfileUseCase.Result(
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
