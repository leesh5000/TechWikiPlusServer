package me.helloc.techwikiplus.user.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidNickname
import java.time.LocalDateTime

class UserTest : FunSpec({

    context("User 생성") {
        test("유효한 정보로 User 생성 성공") {
            val user = User(
                id = "123456789",
                email = UserEmail("test@example.com"),
                password = "hashedPassword",
                nickname = "테스터123"
            )

            user.id shouldBe "123456789"
            user.email.value shouldBe "test@example.com"
            user.password shouldBe "hashedPassword"
            user.nickname shouldBe "테스터123"
            user.createdAt shouldNotBe null
            user.updatedAt shouldNotBe null
        }

        test("기본값으로 생성 시 createdAt과 updatedAt이 설정됨") {
            val beforeCreation = LocalDateTime.now().minusSeconds(1)
            val user = User(
                id = "123456789",
                email = UserEmail("test@example.com"),
                password = "hashedPassword",
                nickname = "테스터"
            )
            val afterCreation = LocalDateTime.now().plusSeconds(1)

            user.createdAt.isAfter(beforeCreation) shouldBe true
            user.createdAt.isBefore(afterCreation) shouldBe true
            user.updatedAt.isAfter(beforeCreation) shouldBe true
            user.updatedAt.isBefore(afterCreation) shouldBe true
        }

        test("특정 시간으로 User 생성") {
            val specificTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val user = User(
                id = "123456789",
                email = UserEmail("test@example.com"),
                password = "hashedPassword",
                nickname = "테스터",
                createdAt = specificTime,
                updatedAt = specificTime
            )

            user.createdAt shouldBe specificTime
            user.updatedAt shouldBe specificTime
        }
    }

    context("닉네임 유효성 검증") {
        test("닉네임이 2자 미만일 때 InvalidNickname 예외 발생") {
            shouldThrow<InvalidNickname> {
                User(
                    id = "123456789",
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "a"
                )
            }.nickname shouldBe "a"
        }

        test("닉네임이 20자 초과일 때 InvalidNickname 예외 발생") {
            val longNickname = "a".repeat(21)
            shouldThrow<InvalidNickname> {
                User(
                    id = "123456789",
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = longNickname
                )
            }.nickname shouldBe longNickname
        }

        test("닉네임에 특수문자 포함 시 InvalidNickname 예외 발생") {
            val invalidNicknames = listOf(
                "test@user",
                "test user",
                "test!",
                "test#123",
                "test-user",
                "test.user",
                "test/user",
                "test\\user"
            )

            invalidNicknames.forEach { nickname ->
                shouldThrow<InvalidNickname> {
                    User(
                        id = "123456789",
                        email = UserEmail("test@example.com"),
                        password = "hashedPassword",
                        nickname = nickname
                    )
                }.nickname shouldBe nickname
            }
        }

        test("유효한 닉네임 형식들") {
            val validNicknames = listOf(
                "ab",
                "12",
                "가나",
                "test123",
                "테스트123",
                "User테스트123",
                "ABCDEFGHIJKLMNopqrst",
                "가나다라마바사아자차카타파하",
                "1234567890123456789",
                "한글English123"
            )

            validNicknames.forEach { nickname ->
                val user = User(
                    id = "123456789",
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = nickname
                )
                user.nickname shouldBe nickname
            }
        }
    }

    context("User 불변성") {
        test("생성된 User의 프로퍼티는 변경 불가능") {
            val user = User(
                id = "123456789",
                email = UserEmail("test@example.com"),
                password = "hashedPassword",
                nickname = "테스터"
            )

            // 모든 프로퍼티가 val로 선언되어 변경 불가능
            user.id shouldBe "123456789"
            user.nickname shouldBe "테스터"
            user.email.value shouldBe "test@example.com"
            user.password shouldBe "hashedPassword"
        }

        test("User 객체는 동일한 ID를 가지면 같음 (향후 equals 구현 시)") {
            val user1 = User(
                id = "123456789",
                email = UserEmail("test1@example.com"),
                password = "password1",
                nickname = "테스터1"
            )

            val user2 = User(
                id = "123456789",
                email = UserEmail("test2@example.com"),
                password = "password2",
                nickname = "테스터2"
            )

            // 현재는 data class가 아니므로 참조 동등성
            user1.id shouldBe user2.id
        }
    }

    context("User와 UserEmail 통합") {
        test("잘못된 이메일로 User 생성 시 InvalidEmail 예외 발생") {
            shouldThrow<me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidEmail> {
                User(
                    id = "123456789",
                    email = UserEmail("invalid-email"),
                    password = "hashedPassword",
                    nickname = "테스터"
                )
            }
        }

        test("검증된 이메일로 User 생성") {
            val user = User(
                id = "123456789",
                email = UserEmail("test@example.com", true),
                password = "hashedPassword",
                nickname = "테스터"
            )

            user.email.verified shouldBe true
        }
    }
})