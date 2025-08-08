package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.web.port.GetMyProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class GetMyProfileFacade(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) : GetMyProfileUseCase {
    override fun execute(): GetMyProfileUseCase.Result {
        val currentUserId = authorizationService.getCurrentUserOrThrow()

        val user = userReader.getActiveUserBy(currentUserId)

        return GetMyProfileUseCase.Result(
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
