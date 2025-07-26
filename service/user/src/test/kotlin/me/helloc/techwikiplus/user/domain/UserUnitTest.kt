package me.helloc.techwikiplus.user.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.user.domain.exception.validation.AlreadyVerifiedEmailException
import me.helloc.techwikiplus.user.domain.exception.validation.InvalidNicknameException
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import java.time.LocalDateTime

class UserUnitTest : FunSpec({

    context("User 생성") {
        test("유효한 정보로 User 생성 성공") {
            val now = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val fakeClock = FakeClock(now)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "테스터123",
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = fakeClock.localDateTime(),
                    updatedAt = fakeClock.localDateTime(),
                )

            user.id shouldBe 123456789L
            user.email.value shouldBe "test@example.com"
            user.password shouldBe "hashedPassword"
            user.nickname shouldBe "테스터123"
            user.status shouldBe UserStatus.ACTIVE
            user.role shouldBe UserRole.USER
            user.createdAt shouldBe now
            user.updatedAt shouldBe now
        }

        test("기본값으로 User 생성") {
            val now = LocalDateTime.of(2024, 1, 1, 10, 0, 0)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "테스터",
                    createdAt = now,
                    updatedAt = now,
                )

            user.status shouldBe UserStatus.ACTIVE
            user.role shouldBe UserRole.USER
        }
    }

    context("닉네임 유효성 검증") {
        test("닉네임이 2자 미만일 때 InvalidNickname 예외 발생") {
            val now = LocalDateTime.now()

            shouldThrow<InvalidNicknameException> {
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "a",
                    createdAt = now,
                    updatedAt = now,
                )
            }.nickname shouldBe "a"
        }

        test("닉네임이 20자 초과일 때 InvalidNickname 예외 발생") {
            val now = LocalDateTime.now()
            val longNickname = "a".repeat(21)

            shouldThrow<InvalidNicknameException> {
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = longNickname,
                    createdAt = now,
                    updatedAt = now,
                )
            }.nickname shouldBe longNickname
        }

        test("닉네임에 특수문자 포함 시 InvalidNickname 예외 발생") {
            val now = LocalDateTime.now()
            val invalidNicknames =
                listOf(
                    "test@user",
                    "test user",
                    "test!",
                    "test#123",
                    "test-user",
                    "test.user",
                    "test/user",
                    "test\\user",
                )

            invalidNicknames.forEach { nickname ->
                shouldThrow<InvalidNicknameException> {
                    User(
                        id = 123456789L,
                        email = UserEmail("test@example.com"),
                        password = "hashedPassword",
                        nickname = nickname,
                        createdAt = now,
                        updatedAt = now,
                    )
                }.nickname shouldBe nickname
            }
        }

        test("유효한 닉네임 형식들") {
            val now = LocalDateTime.now()
            val validNicknames =
                listOf(
                    "ab",
                    "12",
                    "가나",
                    "test123",
                    "테스트123",
                    "User테스트123",
                    // 20자
                    "ABCDEFGHIJKLMNopqrs",
                    "가나다라마바사아자차카타파하",
                    "1234567890123456789",
                    "한글English123",
                )

            validNicknames.forEach { nickname ->
                val user =
                    User(
                        id = 123456789L,
                        email = UserEmail("test@example.com"),
                        password = "hashedPassword",
                        nickname = nickname,
                        createdAt = now,
                        updatedAt = now,
                    )
                user.nickname shouldBe nickname
            }
        }
    }

    context("withPendingUser 팩토리 메서드") {
        test("PENDING 상태의 User 생성") {
            val now = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val fakeClock = FakeClock(now)

            val pendingUser =
                User.withPendingUser(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    nickname = "테스터",
                    password = "hashedPassword",
                    clock = fakeClock,
                )

            pendingUser.id shouldBe 123456789L
            pendingUser.email.value shouldBe "test@example.com"
            pendingUser.nickname shouldBe "테스터"
            pendingUser.password shouldBe "hashedPassword"
            pendingUser.status shouldBe UserStatus.PENDING
            pendingUser.role shouldBe UserRole.USER
            pendingUser.createdAt shouldBe now
            pendingUser.updatedAt shouldBe now
        }

        test("withPendingUser에서도 닉네임 유효성 검증") {
            shouldThrow<InvalidNicknameException> {
                User.withPendingUser(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    nickname = "a",
                    password = "hashedPassword",
                )
            }.nickname shouldBe "a"
        }
    }

    context("changeNickname 메서드") {
        test("유효한 닉네임으로 변경 성공") {
            val createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val updatedAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0)
            val fakeClock = FakeClock(createdAt)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "기존닉네임",
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

            fakeClock.setTime(updatedAt)
            val updatedUser = user.changeNickname("새로운닉네임", fakeClock)

            updatedUser.nickname shouldBe "새로운닉네임"
            updatedUser.id shouldBe user.id
            updatedUser.email shouldBe user.email
            updatedUser.password shouldBe user.password
            updatedUser.createdAt shouldBe createdAt
            updatedUser.updatedAt shouldBe updatedAt
        }

        test("잘못된 닉네임으로 변경 시 예외 발생") {
            val now = LocalDateTime.now()
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "테스터",
                    createdAt = now,
                    updatedAt = now,
                )

            shouldThrow<InvalidNicknameException> {
                user.changeNickname("@")
            }.nickname shouldBe "@"
        }
    }

    context("verifyEmail 메서드") {
        test("이메일 인증 처리") {
            val createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val updatedAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0)
            val fakeClock = FakeClock(createdAt)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com", false),
                    password = "hashedPassword",
                    nickname = "테스터",
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

            fakeClock.setTime(updatedAt)
            val verifiedUser = user.verifyEmail(fakeClock)

            verifiedUser.email.verified shouldBe true
            verifiedUser.updatedAt shouldBe updatedAt
            user.email.verified shouldBe false // 원본은 변경되지 않음
        }
    }

    context("isPending 메서드") {
        test("PENDING 상태인 경우 true 반환") {
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "hashedPassword",
                    nickname = "테스터",
                    status = UserStatus.PENDING,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user.isPending() shouldBe true
        }

        test("PENDING이 아닌 상태인 경우 false 반환") {
            val statuses =
                listOf(
                    UserStatus.ACTIVE,
                    UserStatus.BANNED,
                    UserStatus.DORMANT,
                    UserStatus.DELETED,
                )

            statuses.forEach { status ->
                val user =
                    User(
                        id = 123456789L,
                        email = UserEmail("test@example.com"),
                        password = "hashedPassword",
                        nickname = "테스터",
                        status = status,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    )

                user.isPending() shouldBe false
            }
        }
    }

    context("completeSignUp 메서드") {
        test("회원가입 완료 처리 - 이메일 인증 및 ACTIVE 상태 변경") {
            val createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val updatedAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0)
            val fakeClock = FakeClock(createdAt)

            val pendingUser =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com", false),
                    password = "hashedPassword",
                    nickname = "테스터",
                    status = UserStatus.PENDING,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )

            fakeClock.setTime(updatedAt)
            val completedUser = pendingUser.completeSignUp(fakeClock)

            completedUser.email.verified shouldBe true
            completedUser.status shouldBe UserStatus.ACTIVE
            completedUser.updatedAt shouldBe updatedAt

            // 원본은 변경되지 않음
            pendingUser.email.verified shouldBe false
            pendingUser.status shouldBe UserStatus.PENDING
        }

        test("이미 활성화된 사용자가 completeSignUp 호출 시 AlreadyVerifiedEmail 예외 발생") {
            val activeUser =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com", true),
                    password = "hashedPassword",
                    nickname = "테스터",
                    status = UserStatus.ACTIVE,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            shouldThrow<AlreadyVerifiedEmailException> {
                activeUser.completeSignUp()
            }.apply {
                email shouldBe "test@example.com"
                message shouldContain "Email is already verified"
            }
        }
    }

    context("copy 메서드") {
        test("모든 필드를 변경할 수 있음") {
            val originalTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val updatedTime = LocalDateTime.of(2024, 1, 2, 10, 0, 0)
            val fakeClock = FakeClock(updatedTime)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password1",
                    nickname = "테스터1",
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = originalTime,
                    updatedAt = originalTime,
                )

            val copiedUser =
                user.copy(
                    id = 987654321L,
                    nickname = "테스터2",
                    email = UserEmail("new@example.com"),
                    password = "password2",
                    status = UserStatus.BANNED,
                    role = UserRole.ADMIN,
                    clock = fakeClock,
                )

            copiedUser.id shouldBe 987654321L
            copiedUser.nickname shouldBe "테스터2"
            copiedUser.email.value shouldBe "new@example.com"
            copiedUser.password shouldBe "password2"
            copiedUser.status shouldBe UserStatus.BANNED
            copiedUser.role shouldBe UserRole.ADMIN
            copiedUser.createdAt shouldBe originalTime // createdAt은 변경되지 않음
            copiedUser.updatedAt shouldBe updatedTime
        }

        test("일부 필드만 변경") {
            val originalTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val updatedTime = LocalDateTime.of(2024, 1, 2, 10, 0, 0)
            val fakeClock = FakeClock(updatedTime)

            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = originalTime,
                    updatedAt = originalTime,
                )

            val copiedUser = user.copy(nickname = "새닉네임", clock = fakeClock)

            copiedUser.id shouldBe user.id
            copiedUser.email shouldBe user.email
            copiedUser.password shouldBe user.password
            copiedUser.nickname shouldBe "새닉네임"
            copiedUser.status shouldBe user.status
            copiedUser.role shouldBe user.role
            copiedUser.updatedAt shouldBe updatedTime
        }
    }

    context("equals와 hashCode") {
        test("동일한 ID를 가진 User는 동등") {
            val user1 =
                User(
                    id = 123456789L,
                    email = UserEmail("test1@example.com"),
                    password = "password1",
                    nickname = "테스터1",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            val user2 =
                User(
                    id = 123456789L,
                    email = UserEmail("test2@example.com"),
                    password = "password2",
                    nickname = "테스터2",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user1 shouldBe user2
            user1.hashCode() shouldBe user2.hashCode()
        }

        test("다른 ID를 가진 User는 동등하지 않음") {
            val user1 =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            val user2 =
                User(
                    id = 987654321L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user1 shouldNotBe user2
        }

        test("자기 자신과는 항상 동등") {
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user shouldBe user
        }

        test("null과는 동등하지 않음") {
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user.equals(null) shouldBe false
        }

        test("다른 타입과는 동등하지 않음") {
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user.equals("not a user") shouldBe false
            user.equals(123456789L) shouldBe false
        }
    }

    context("email 메서드") {
        test("이메일 문자열 반환") {
            val user =
                User(
                    id = 123456789L,
                    email = UserEmail("test@example.com"),
                    password = "password",
                    nickname = "테스터",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            user.getEmailValue() shouldBe "test@example.com"
        }
    }
})
