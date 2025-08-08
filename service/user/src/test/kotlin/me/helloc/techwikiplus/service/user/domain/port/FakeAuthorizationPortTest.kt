package me.helloc.techwikiplus.service.user.domain.port

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

class FakeAuthorizationPortTest : DescribeSpec({
    lateinit var authorizationPort: FakeAuthorizationPort

    beforeEach {
        authorizationPort = FakeAuthorizationPort()
    }

    describe("getCurrentUserId") {
        it("사용자가 설정되지 않았을 때 null을 반환한다") {
            authorizationPort.getCurrentUserId() shouldBe null
        }

        it("사용자가 설정되었을 때 현재 사용자 ID를 반환한다") {
            val userId = UserId("test-user-id")
            authorizationPort.setCurrentUser(userId)

            authorizationPort.getCurrentUserId() shouldBe userId
        }
    }

    describe("requireAuthenticated") {
        it("사용자가 설정되지 않았을 때 UNAUTHORIZED 예외를 발생시킨다") {
            val exception =
                shouldThrow<DomainException> {
                    authorizationPort.requireAuthenticated()
                }

            exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
        }

        it("사용자가 설정되었을 때 현재 사용자 ID를 반환한다") {
            val userId = UserId("test-user-id")
            authorizationPort.setCurrentUser(userId)

            authorizationPort.requireAuthenticated() shouldBe userId
        }
    }

    describe("isAuthenticated") {
        it("사용자가 설정되지 않았을 때 false를 반환한다") {
            authorizationPort.isAuthenticated() shouldBe false
        }

        it("사용자가 설정되었을 때 true를 반환한다") {
            authorizationPort.setCurrentUser(UserId("test-user-id"))

            authorizationPort.isAuthenticated() shouldBe true
        }
    }

    describe("hasRole") {
        it("인증되지 않았을 때 false를 반환한다") {
            authorizationPort.hasRole(UserRole.USER) shouldBe false
        }

        it("인증되었고 역할이 일치할 때 true를 반환한다") {
            authorizationPort.setCurrentUser(UserId("test-user-id"), UserRole.ADMIN)

            authorizationPort.hasRole(UserRole.ADMIN) shouldBe true
        }

        it("인증되었지만 역할이 일치하지 않을 때 false를 반환한다") {
            authorizationPort.setCurrentUser(UserId("test-user-id"), UserRole.USER)

            authorizationPort.hasRole(UserRole.ADMIN) shouldBe false
        }
    }

    describe("canAccessUser") {
        it("인증되지 않았을 때 false를 반환한다") {
            authorizationPort.canAccessUser(UserId("target-user-id")) shouldBe false
        }

        it("자신의 프로필에 접근할 때 true를 반환한다") {
            val userId = UserId("test-user-id")
            authorizationPort.setCurrentUser(userId)

            authorizationPort.canAccessUser(userId) shouldBe true
        }

        it("일반 사용자가 다른 사용자의 프로필에 접근할 때 false를 반환한다") {
            authorizationPort.setCurrentUser(UserId("test-user-id"), UserRole.USER)

            authorizationPort.canAccessUser(UserId("other-user-id")) shouldBe false
        }

        it("관리자가 모든 사용자의 프로필에 접근할 때 true를 반환한다") {
            authorizationPort.setCurrentUser(UserId("admin-user-id"), UserRole.ADMIN)

            authorizationPort.canAccessUser(UserId("other-user-id")) shouldBe true
        }
    }

    describe("clearCurrentUser") {
        it("모든 인증 상태를 초기화한다") {
            authorizationPort.setCurrentUser(UserId("test-user-id"), UserRole.ADMIN)
            authorizationPort.clearCurrentUser()

            authorizationPort.getCurrentUserId() shouldBe null
            authorizationPort.isAuthenticated() shouldBe false
            authorizationPort.hasRole(UserRole.ADMIN) shouldBe false
        }
    }
})
