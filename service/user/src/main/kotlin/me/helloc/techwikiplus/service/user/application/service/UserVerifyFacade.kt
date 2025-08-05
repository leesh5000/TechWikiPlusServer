package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.application.port.inbound.UserVerifyUseCase
import me.helloc.techwikiplus.service.user.application.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@Component
class UserVerifyFacade(
    private val userReader: UserReader,
    private val userWriter: UserWriter,
    private val verificationCodeStore: VerificationCodeStore,
    private val auditor: Auditor,
) : UserVerifyUseCase {
    override fun execute(command: UserVerifyUseCase.Command) {
        // 1. 사용자 조회 (먼저 수행)
        val user = userReader.getPendingUserBy(command.email)

        // 2. 인증 코드가 유효한지 확인
        verificationCodeStore.equalsOrThrows(command.email, command.code)

        // 3. 사용자 상태를 ACTIVE로 변경
        val modifiedAt: Instant = auditor.generateModifyTime()
        val activatedUser = user.activate(modifiedAt)

        // 4. 변경된 사용자 저장
        userWriter.update(activatedUser)
    }
}
