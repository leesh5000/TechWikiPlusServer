package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.LockManager
import me.helloc.techwikiplus.service.user.domain.service.port.LockManagerException
import me.helloc.techwikiplus.service.user.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.FakePasswordEncryptor
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserIdGenerator
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.service.user.infrastructure.SlowLockManager
import me.helloc.techwikiplus.service.user.infrastructure.TestLockManager
import me.helloc.techwikiplus.service.user.infrastructure.TrackingLockManager
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class UserRegisterTest : DescribeSpec({

    /**
     * 테스트용 UserRegister 생성 헬퍼 메서드
     * FIRST 원칙의 Independent를 위해 매번 새로운 인스턴스 생성
     */
    fun createUserRegister(
        repository: FakeUserRepository = FakeUserRepository(),
        clockTime: Instant = Instant.parse("2025-01-07T10:00:00Z"),
        lockManager: LockManager = NoOpLockManager(),
        userIdGenerator: FakeUserIdGenerator = FakeUserIdGenerator(),
    ): UserRegister {
        return UserRegister(
            clockHolder = FakeClockHolder(clockTime),
            userIdGenerator = userIdGenerator,
            repository = repository,
            passwordEncryptor = FakePasswordEncryptor(),
            lockManager = lockManager,
        )
    }

    describe("UserRegister 기본 기능 테스트") {

        context("정상적인 사용자 등록") {
            it("유효한 정보로 사용자를 성공적으로 등록한다") {
                // Given: 유효한 사용자 정보와 UserRegister 준비
                val repository = FakeUserRepository()
                val userRegister = createUserRegister(repository = repository)
                val email = Email("test@example.com")
                val nickname = Nickname("testuser")
                val password = RawPassword("Password123!")
                val passwordConfirm = RawPassword("Password123!")

                // When: 사용자 등록 실행
                val result = userRegister.insert(email, nickname, password, passwordConfirm)

                // Then: 사용자가 성공적으로 등록되었는지 검증
                result shouldNotBe null
                result.id shouldBe UserId(1000000L)
                result.email shouldBe email
                result.nickname shouldBe nickname
                result.encodedPassword shouldBe EncodedPassword("encoded_Password123!")
                result.status shouldBe UserStatus.PENDING
                result.createdAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
                result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

                val savedUser = repository.findBy(email)
                savedUser shouldNotBe null
                savedUser?.id shouldBe result.id
            }
        }

        context("비밀번호 검증 실패") {
            it("비밀번호와 비밀번호 확인이 일치하지 않으면 PASSWORD_MISMATCH 예외를 발생시킨다") {
                // Given: 불일치하는 비밀번호 정보 준비
                val repository = FakeUserRepository()
                val userRegister = createUserRegister(repository = repository)
                val email = Email("test@example.com")
                val nickname = Nickname("testuser")
                val password = RawPassword("Password123!")
                val passwordConfirm = RawPassword("DifferentPassword123!")

                // When & Then: 등록 시도 시 PASSWORD_MISMATCH 예외 발생
                val exception =
                    shouldThrow<UserDomainException> {
                        userRegister.insert(email, nickname, password, passwordConfirm)
                    }
                exception.userErrorCode shouldBe UserErrorCode.PASSWORD_MISMATCH

                // Then: 저장소에 저장되지 않았는지 확인
                repository.getAll().size shouldBe 0
            }
        }

        context("중복 이메일 검증") {
            it("이미 존재하는 이메일로 등록하면 DUPLICATE_EMAIL 예외를 발생시킨다") {
                // Given: 기존 사용자가 이미 존재하는 상황 준비
                val repository = FakeUserRepository()
                val userRegister = createUserRegister(repository = repository)
                val existingEmail = Email("existing@example.com")
                val existingUser =
                    User.create(
                        id = UserId(2000001L),
                        email = existingEmail,
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("existinguser"),
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                repository.save(existingUser)

                val newNickname = Nickname("newuser")
                val password = RawPassword("Password123!")

                // When & Then: 동일한 이메일로 등록 시도 시 DUPLICATE_EMAIL 예외 발생
                val exception =
                    shouldThrow<UserDomainException> {
                        userRegister.insert(existingEmail, newNickname, password, password)
                    }
                exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_EMAIL
                exception.params shouldBe arrayOf(existingEmail.value)

                // Then: 새로운 사용자가 저장되지 않았는지 확인
                repository.getAll().size shouldBe 1
            }
        }

        context("중복 닉네임 검증") {
            it("이미 존재하는 닉네임으로 등록하면 DUPLICATE_NICKNAME 예외를 발생시킨다") {
                // Given: 기존 닉네임이 이미 존재하는 상황 준비
                val repository = FakeUserRepository()
                val userRegister = createUserRegister(repository = repository)
                val existingNickname = Nickname("existinguser")
                val existingUser =
                    User.create(
                        id = UserId(2000001L),
                        email = Email("existing@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = existingNickname,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                repository.save(existingUser)

                val newEmail = Email("new@example.com")
                val password = RawPassword("Password123!")

                // When & Then: 동일한 닉네임으로 등록 시도 시 DUPLICATE_NICKNAME 예외 발생
                val exception =
                    shouldThrow<UserDomainException> {
                        userRegister.insert(newEmail, existingNickname, password, password)
                    }
                exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_NICKNAME
                exception.params shouldBe arrayOf(existingNickname.value)

                // Then: 새로운 사용자가 저장되지 않았는지 확인
                repository.getAll().size shouldBe 1
            }
        }

        context("순차 다중 사용자 등록") {
            it("여러 사용자를 순차적으로 등록할 수 있다") {
                // Given: 두 명의 서로 다른 사용자 정보 준비
                val repository = FakeUserRepository()
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
                val userIdGenerator = FakeUserIdGenerator()
                val userRegister =
                    createUserRegister(
                        repository = repository,
                        clockTime = clockHolder.now(),
                        userIdGenerator = userIdGenerator,
                    )

                val user1Email = Email("user1@example.com")
                val user1Nickname = Nickname("user1")
                val user1Password = RawPassword("Password123!")

                val user2Email = Email("user2@example.com")
                val user2Nickname = Nickname("user2")
                val user2Password = RawPassword("Password456!")

                // When: 두 사용자를 순차적으로 등록
                val user1 = userRegister.insert(user1Email, user1Nickname, user1Password, user1Password)

                // 시간 경과 시뮬레이션
                clockHolder.advanceTimeBySeconds(60)
                val userRegister2 =
                    createUserRegister(
                        repository = repository,
                        clockTime = clockHolder.now(),
                        userIdGenerator = userIdGenerator,
                    )
                val user2 = userRegister2.insert(user2Email, user2Nickname, user2Password, user2Password)

                // Then: 두 사용자 모두 성공적으로 등록됨
                user1.id shouldBe UserId(1000000L)
                user2.id shouldBe UserId(1000001L)

                repository.getAll().size shouldBe 2
                repository.findBy(user1Email) shouldBe user1
                repository.findBy(user2Email) shouldBe user2
            }
        }

        context("사용자 정보 업데이트") {
            it("기존 사용자 정보를 성공적으로 업데이트한다") {
                // Given: 기존 사용자와 업데이트할 정보 준비
                val repository = FakeUserRepository()
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
                val userRegister = createUserRegister(repository = repository, clockTime = clockHolder.now())

                val originalUser =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("originalname"),
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                repository.save(originalUser)

                val updatedUser =
                    originalUser.copy(
                        nickname = Nickname("updatedname"),
                        modifiedAt = clockHolder.now(),
                    )

                // When: 사용자 정보 업데이트 실행
                val result = userRegister.update(updatedUser)

                // Then: 사용자 정보가 성공적으로 업데이트됨
                result shouldBe updatedUser
                val savedUser = repository.findBy(UserId(1000001L))
                savedUser shouldNotBe null
                savedUser?.nickname shouldBe Nickname("updatedname")
                savedUser?.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
            }

            it("존재하지 않는 사용자도 저장할 수 있다") {
                // Given: 새로운 사용자 정보 준비
                val repository = FakeUserRepository()
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
                val userRegister = createUserRegister(repository = repository, clockTime = clockHolder.now())

                val newUser =
                    User.create(
                        id = UserId(3000001L),
                        email = Email("new@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("newuser"),
                        createdAt = clockHolder.now(),
                        modifiedAt = clockHolder.now(),
                    )

                // When: 새로운 사용자 저장 실행
                val result = userRegister.update(newUser)

                // Then: 새로운 사용자가 성공적으로 저장됨
                result shouldBe newUser
                repository.findBy(UserId(3000001L)) shouldBe newUser
                repository.getAll().size shouldBe 1
            }

            it("사용자 상태를 변경할 수 있다") {
                // Given: 기존 활성 사용자와 상태 변경 정보 준비
                val repository = FakeUserRepository()
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
                val userRegister = createUserRegister(repository = repository, clockTime = clockHolder.now())

                val activeUser =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("testuser"),
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                repository.save(activeUser)

                val dormantUser =
                    activeUser.copy(
                        status = UserStatus.DORMANT,
                        modifiedAt = clockHolder.now(),
                    )

                // When: 사용자 상태 변경 실행
                val result = userRegister.update(dormantUser)

                // Then: 사용자 상태가 성공적으로 변경됨
                result.status shouldBe UserStatus.DORMANT
                val savedUser = repository.findBy(UserId(1000001L))
                savedUser?.status shouldBe UserStatus.DORMANT
            }
        }
    }

    describe("UserRegister 동시성 테스트") {

        context("분산 락 없는 동시 등록") {
            it("동일 이메일로 동시 요청 시 race condition 발생 시뮬레이션") {
                // Given: NoOpDistributedLock을 사용하는 UserRegister와 동시 요청 준비
                val repository = FakeUserRepository()
                val userRegisterWithoutLock =
                    createUserRegister(
                        repository = repository,
                        lockManager = NoOpLockManager(),
                    )

                val email = Email("concurrent@test.com")
                val nickname1 = Nickname("user1")
                val nickname2 = Nickname("user2")
                val password = RawPassword("Password123!")

                val successCount = AtomicInteger(0)
                val exceptionCount = AtomicInteger(0)
                val latch = CountDownLatch(2)

                // When: 동일한 이메일로 2개의 동시 등록 요청 실행
                CompletableFuture.runAsync {
                    try {
                        userRegisterWithoutLock.insert(email, nickname1, password, password)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        exceptionCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }

                CompletableFuture.runAsync {
                    try {
                        userRegisterWithoutLock.insert(email, nickname2, password, password)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        exceptionCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }

                latch.await(5, TimeUnit.SECONDS)

                // Then: FakeRepository의 중복 검사에 의해 하나만 성공
                successCount.get() shouldBe 1
                exceptionCount.get() shouldBe 1
                repository.findBy(email) shouldNotBe null
            }
        }

        context("분산 락 기반 동시 등록") {
            it("동일 이메일로 동시 요청 시 락으로 순차 처리된다") {
                // Given: TestDistributedLock을 사용하는 UserRegister와 동시 요청 준비
                val repository = FakeUserRepository()
                val lockManager = TestLockManager()
                val userRegister =
                    createUserRegister(
                        repository = repository,
                        lockManager = lockManager,
                    )

                val email = Email("locked@test.com")
                val nickname1 = Nickname("user1")
                val nickname2 = Nickname("user2")
                val password = RawPassword("Password123!")

                val latch = CountDownLatch(2)
                val results = mutableListOf<Exception?>()

                // When: 동일한 이메일로 2개의 동시 등록 요청 실행
                CompletableFuture.supplyAsync {
                    try {
                        userRegister.insert(email, nickname1, password, password)
                        null
                    } catch (e: Exception) {
                        e
                    }.also {
                        synchronized(results) { results.add(it) }
                        latch.countDown()
                    }
                }

                CompletableFuture.supplyAsync {
                    try {
                        userRegister.insert(email, nickname2, password, password)
                        null
                    } catch (e: Exception) {
                        e
                    }.also {
                        synchronized(results) { results.add(it) }
                        latch.countDown()
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // Then: 하나는 성공하고 하나는 중복 이메일 예외 발생
                val successCount = results.count { it == null }
                val duplicateEmailCount =
                    results.count {
                        it is UserDomainException && it.userErrorCode == UserErrorCode.DUPLICATE_EMAIL
                    }

                successCount shouldBe 1
                duplicateEmailCount shouldBe 1
                repository.findBy(email) shouldNotBe null
            }
        }

        context("락 타임아웃 시나리오") {
            it("락 획득 타임아웃 시 LockManagerException이 발생한다") {
                // Given: 항상 타임아웃되는 SlowLockManager 사용
                val slowLockManager = SlowLockManager()
                val userRegister = createUserRegister(lockManager = slowLockManager)

                val email = Email("timeout@test.com")
                val nickname = Nickname("timeoutuser")
                val password = RawPassword("Password123!")

                // When & Then: 등록 시도 시 LockManagerException 발생
                val exception =
                    runCatching {
                        userRegister.insert(email, nickname, password, password)
                    }.exceptionOrNull()

                exception shouldNotBe null
                exception.shouldBeInstanceOf<LockManagerException>()
            }
        }

        context("락 키 패턴 검증") {
            it("회원가입 시 올바른 락 키가 사용되는지 확인한다") {
                // Given: 락 사용을 추적하는 TrackingLockManager 사용
                val trackingLock = TrackingLockManager()
                val userRegister = createUserRegister(lockManager = trackingLock)

                val email = Email("track@test.com")
                val nickname = Nickname("trackuser")
                val password = RawPassword("Password123!")
                val expectedLockKey = "user:register:${email.value}"

                // When: 사용자 등록 실행
                userRegister.insert(email, nickname, password, password)

                // Then: 예상된 락 키가 정확히 1번 사용됨
                trackingLock.getLockCount(expectedLockKey) shouldBe 1
            }
        }

        context("고부하 동시성 테스트") {
            it("10개의 동시 요청 중 하나만 성공해야 한다") {
                // Given: 동일한 이메일로 10개의 동시 요청 준비
                val repository = FakeUserRepository()
                val lockManager = TestLockManager()
                val userRegister =
                    createUserRegister(
                        repository = repository,
                        lockManager = lockManager,
                    )

                val email = Email("concurrent10@test.com")
                val password = RawPassword("Password123!")
                val threadCount = 10
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val exceptionCount = AtomicInteger(0)

                // When: 10개의 서로 다른 닉네임으로 동시 등록 요청 실행
                (1..threadCount).map { i ->
                    CompletableFuture.runAsync {
                        try {
                            val nickname = Nickname("user$i")
                            userRegister.insert(email, nickname, password, password)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            exceptionCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await(15, TimeUnit.SECONDS)

                // Then: 하나만 성공하고 나머지는 중복 이메일 예외
                successCount.get() shouldBe 1
                exceptionCount.get() shouldBe threadCount - 1
                repository.findBy(email) shouldNotBe null
            }
        }
    }
})
