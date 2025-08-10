package me.helloc.techwikiplus.service.user

import me.helloc.techwikiplus.service.user.domain.model.value.UserId

object TestFixtures {
    // 테스트용 UserId 생성 헬퍼
    fun createUserId(id: Long = 1000000L): UserId = UserId(id)
    
    // 일반적으로 사용되는 테스트 ID들
    val TEST_USER_ID_1 = createUserId(1000001L)
    val TEST_USER_ID_2 = createUserId(1000002L)
    val EXISTING_USER_ID = createUserId(2000001L)
    val NEW_USER_ID = createUserId(3000001L)
}