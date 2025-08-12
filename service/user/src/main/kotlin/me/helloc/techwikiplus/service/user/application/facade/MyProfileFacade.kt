package me.helloc.techwikiplus.service.user.application.facade

import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.web.port.MyProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class MyProfileFacade(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) : MyProfileUseCase {
    override fun execute(): MyProfileUseCase.Result {
        val currentUserId = authorizationService.getCurrentUserOrThrow()
        val user = userReader.get(currentUserId)

        return MyProfileUseCase.Result(
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
