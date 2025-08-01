package me.helloc.techwikiplus.service.user.unit.application

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.application.UserSignUpFacade
import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.type.UserRole
import me.helloc.techwikiplus.domain.model.type.UserStatus
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.UserReader
import me.helloc.techwikiplus.infrastructure.persistence.FakeUserRepository

class UserSignUpFacadeTest : DescribeSpec({

    describe("회원가입 시") {

        val reader: UserReader = UserReader(
            repository = FakeUserRepository(),
        )
        val userSignUpFacade = UserSignUpFacade(
            reader = reader,
        )

        context("올바른 이메일, 비밀번호, 비밀번호 확인, 닉네임을 입력했을 때") {

            val email = "test@example.com"
            val password = "password123!"
            val confirmPassword = "password123!"
            val nickname = "TestUser"
            var result: Boolean = false

            beforeSpec {
                result = userSignUpFacade.signup(
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    nickname = nickname,
                )
            }

            it("회원가입이 성공해야 한다") {
                // Then
                result shouldBe true
            }

            it("상태가 PENDING인 유저가 생성되어야 한다") {

                // Then
                val userResult: DomainResult<User> = reader.findByEmail(Email(email))

                // 예외가 발생하지 않음을 검증
                val user: User = shouldNotThrowAny(
                    block = { userResult.getOrThrow() }
                )
                user shouldNotBe null
                user.email.value shouldBeEqual email
                user.nickname.value shouldBeEqual nickname
                user.status shouldBeEqual UserStatus.PENDING
                user.role shouldBeEqual UserRole.USER


                user.getOrNull()?.email?.value shouldBe email
                user.getOrNull()?.nickname?.value shouldBe nickname
                user.getOrNull()?.status?.name shouldBe "PENDING"
            }
        }

        context("비밀번호와 비밀번호 확인이 일치하지 않을 때") {
            it("회원가입이 실패해야 한다") {
                // Given
                val email = "test@example.com"
                val password = "password123!"
                val confirmPassword = "differentPassword!"
                val nickname = "TestUser"

                // When
                val result =
                    userSignUpFacade.signup(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    )

                // Then
                result shouldBe false
            }
        }

        context("잘못된 이메일 형식을 입력했을 때") {
            it("회원가입이 실패해야 한다") {
                // Given
                val email = "invalid-email"
                val password = "password123!"
                val confirmPassword = "password123!"
                val nickname = "TestUser"

                // When
                val result =
                    userSignUpFacade.signup(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    )

                // Then
                result shouldBe false
            }
        }

        context("이미 존재하는 이메일로 가입을 시도했을 때") {
            it("회원가입이 실패해야 한다") {
                // Given
                val existingEmail = "existing@example.com"
                val password = "password123!"
                val confirmPassword = "password123!"
                val nickname = "NewUser"

                // 기존 사용자 먼저 가입 (실제 구현에서는 repository에 저장)
                userSignUpFacade.signup(existingEmail, password, confirmPassword, "ExistingUser")

                // When
                val result =
                    userSignUpFacade.signup(
                        email = existingEmail,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    )

                // Then
                result shouldBe false
            }
        }

        context("빈 닉네임을 입력했을 때") {
            it("회원가입이 실패해야 한다") {
                // Given
                val email = "test@example.com"
                val password = "password123!"
                val confirmPassword = "password123!"
                val nickname = ""

                // When
                val result =
                    userSignUpFacade.signup(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nickname = nickname,
                    )

                // Then
                result shouldBe false
            }
        }
    }
})
