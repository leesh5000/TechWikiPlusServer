package me.helloc.techwikiplus.service.user.application

import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserVerifyUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserVerifyFacade(
    private val userReader: UserReader,
    private val userWriter: UserWriter,
    private val verificationCodeStore: VerificationCodeStore,
    private val auditor: Auditor,
) : UserVerifyUseCase {
    override fun verify(
        email: Email,
        code: VerificationCode,
    ) {
        // 1. 사용자 조회 (먼저 수행)
        val user = userReader.getBy(email)

        // 2. 캐시에서 저장된 인증 코드 조회 (없으면 예외 발생)
        val storedCode = verificationCodeStore.get(email)

        // 3. 인증 코드가 일치하지 않으면 예외 발생
        if (storedCode != code) {
            throw InvalidVerificationCodeException()
        }

        // 4. 사용자 상태를 ACTIVE로 변경
        val activatedUser =
            user.copy(
                status = UserStatus.ACTIVE,
                modifiedAt = auditor.generateModifyTime(),
            )

        // 5. 변경된 사용자 저장
        userWriter.update(activatedUser)
    }
}
