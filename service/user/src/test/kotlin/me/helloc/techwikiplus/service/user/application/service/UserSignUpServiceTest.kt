package me.helloc.techwikiplus.service.user.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.CacheStore
import me.helloc.techwikiplus.service.user.domain.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.port.IdGenerator
import me.helloc.techwikiplus.service.user.domain.port.MailSender
import me.helloc.techwikiplus.service.user.domain.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserRegister
import java.time.Duration
import java.time.Instant

class UserSignUpServiceTest : FunSpec({

    lateinit var userRegister: FakeUserRegister
    lateinit var emailVerifyService: FakeEmailVerifyService
    lateinit var userModifier: FakeUserModifier
    lateinit var service: UserSignUpService

    beforeEach {
        userRegister = FakeUserRegister()
        emailVerifyService = FakeEmailVerifyService()
        userModifier = FakeUserModifier()
        
        service = UserSignUpService(
            userRegister = userRegister,
            emailVerifyService = emailVerifyService,
            userModifier = userModifier
        )
    }

    afterEach {
        userRegister.clear()
        emailVerifyService.clear()
        userModifier.clear()
    }

    context("execute 메서드") {
        
        test("유효한 데이터로 회원가입하면 성공적으로 처리된다") {
            val email = Email("test@example.com")
            val nickname = Nickname("testuser")
            val password = RawPassword("Password123!")
            val confirmPassword = RawPassword("Password123!")
            
            service.execute(email, nickname, password, confirmPassword)
            
            userRegister.insertedUser shouldNotBe null
            userRegister.insertCallCount shouldBe 1
            
            emailVerifyService.verificationStarted shouldBe true
            emailVerifyService.verifiedUser shouldNotBe null
            
            userModifier.pendingSetCount shouldBe 1
            userModifier.modifiedUser shouldNotBe null
        }

        test("비밀번호가 일치하지 않으면 DomainException이 발생한다") {
            val email = Email("test@example.com")
            val nickname = Nickname("testuser")
            val password = RawPassword("Password123!")
            val confirmPassword = RawPassword("DifferentPassword456!")
            
            userRegister.shouldThrowPasswordMismatch = true
            
            val exception = shouldThrow<DomainException> {
                service.execute(email, nickname, password, confirmPassword)
            }
            
            exception.errorCode shouldBe ErrorCode.PASSWORDS_MISMATCH
            emailVerifyService.verificationStarted shouldBe false
            userModifier.pendingSetCount shouldBe 0
        }

        test("이미 존재하는 이메일로 가입 시도하면 DomainException이 발생한다") {
            val email = Email("existing@example.com")
            val nickname = Nickname("newuser")
            val password = RawPassword("Password123!")
            val confirmPassword = RawPassword("Password123!")
            
            userRegister.emailExists = true
            
            val exception = shouldThrow<DomainException> {
                service.execute(email, nickname, password, confirmPassword)
            }
            
            exception.errorCode shouldBe ErrorCode.USER_ALREADY_EXISTS
            emailVerifyService.verificationStarted shouldBe false
            userModifier.pendingSetCount shouldBe 0
        }

        test("이미 존재하는 닉네임으로 가입 시도하면 DomainException이 발생한다") {
            val email = Email("test@example.com")
            val nickname = Nickname("existinguser")
            val password = RawPassword("Password123!")
            val confirmPassword = RawPassword("Password123!")
            
            userRegister.nicknameExists = true
            
            val exception = shouldThrow<DomainException> {
                service.execute(email, nickname, password, confirmPassword)
            }
            
            exception.errorCode shouldBe ErrorCode.USER_ALREADY_EXISTS
            emailVerifyService.verificationStarted shouldBe false
            userModifier.pendingSetCount shouldBe 0
        }

        test("회원가입 후 이메일 인증 시작과 사용자 상태 변경이 순서대로 실행된다") {
            val email = Email("test@example.com")
            val nickname = Nickname("testuser")
            val password = RawPassword("Password123!")
            val confirmPassword = RawPassword("Password123!")
            
            val executionOrder = mutableListOf<String>()
            
            userRegister.onInsertCallback = { executionOrder.add("register") }
            emailVerifyService.onStartCallback = { executionOrder.add("verify") }
            userModifier.onSetPendingCallback = { executionOrder.add("pending") }
            
            service.execute(email, nickname, password, confirmPassword)
            
            executionOrder shouldBe listOf("register", "verify", "pending")
        }
    }
})

class FakeUserRegister(
    private val fakeClockHolder: FakeClockHolder = FakeClockHolder(),
    private val fakeIdGenerator: FakeIdGenerator = FakeIdGenerator(),
    private val fakeRepository: FakeUserRepository = FakeUserRepository(),
    private val fakePasswordEncryptor: FakePasswordEncryptor = FakePasswordEncryptor()
) : UserRegister(
    clockHolder = fakeClockHolder,
    idGenerator = fakeIdGenerator,
    repository = fakeRepository,
    passwordEncryptor = fakePasswordEncryptor
) {
    var insertCallCount = 0
    var insertedUser: User? = null
    var shouldThrowPasswordMismatch = false
    var emailExists = false
    var nicknameExists = false
    var onInsertCallback: (() -> Unit)? = null

    override fun insert(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        passwordConfirm: RawPassword
    ): User {
        insertCallCount++
        onInsertCallback?.invoke()
        
        if (shouldThrowPasswordMismatch && password != passwordConfirm) {
            throw DomainException(ErrorCode.PASSWORDS_MISMATCH)
        }
        
        if (emailExists) {
            fakeRepository.setEmailExists(true)
        }
        
        if (nicknameExists) {
            fakeRepository.setNicknameExists(true)
        }
        
        val user = super.insert(email, nickname, password, passwordConfirm)
        insertedUser = user
        return user
    }

    fun clear() {
        insertCallCount = 0
        insertedUser = null
        shouldThrowPasswordMismatch = false
        emailExists = false
        nicknameExists = false
        onInsertCallback = null
        fakeRepository.clear()
    }
}

class FakeEmailVerifyService : EmailVerifyService(
    mailSender = FakeMailSender(),
    cacheStore = FakeCacheStore()
) {
    var verificationStarted = false
    var verifiedUser: User? = null
    var onStartCallback: (() -> Unit)? = null

    override fun startVerification(user: User) {
        verificationStarted = true
        verifiedUser = user
        onStartCallback?.invoke()
        super.startVerification(user)
    }

    fun clear() {
        verificationStarted = false
        verifiedUser = null
        onStartCallback = null
    }
}

class FakeUserModifier : UserModifier(
    clockHolder = FakeClockHolder(),
    repository = FakeUserRepository()
) {
    var pendingSetCount = 0
    var modifiedUser: User? = null
    var onSetPendingCallback: (() -> Unit)? = null

    override fun setPending(user: User): User {
        pendingSetCount++
        onSetPendingCallback?.invoke()
        val result = super.setPending(user)
        modifiedUser = result
        return result
    }

    fun clear() {
        pendingSetCount = 0
        modifiedUser = null
        onSetPendingCallback = null
    }
}

class FakeClockHolder : ClockHolder {
    private var currentTime = Instant.now()
    
    override fun now(): Instant = currentTime
    override fun nowEpochMilli(): Long = currentTime.toEpochMilli()
    override fun nowEpochSecond(): Int = currentTime.epochSecond.toInt()
    
    fun setTime(time: Instant) {
        currentTime = time
    }
}

class FakeIdGenerator : IdGenerator {
    private var counter = 0
    
    override fun next(): UserId = UserId("test-id-${++counter}")
}

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private var emailExistsFlag = false
    private var nicknameExistsFlag = false
    
    override fun findBy(userId: UserId): User? = users[userId.value]
    
    override fun findBy(email: Email): User? = users.values.find { it.email == email }
    
    override fun findBy(email: Email, status: UserStatus): User? = 
        users.values.find { it.email == email && it.status == status }
    
    override fun exists(email: Email): Boolean = 
        emailExistsFlag || users.values.any { it.email == email }
    
    override fun exists(nickname: Nickname): Boolean = 
        nicknameExistsFlag || users.values.any { it.nickname == nickname }
    
    override fun save(user: User): User {
        users[user.id.value] = user
        return user
    }
    
    fun setEmailExists(exists: Boolean) {
        emailExistsFlag = exists
    }
    
    fun setNicknameExists(exists: Boolean) {
        nicknameExistsFlag = exists
    }
    
    fun clear() {
        users.clear()
        emailExistsFlag = false
        nicknameExistsFlag = false
    }
}

class FakePasswordEncryptor : PasswordEncryptor {
    override fun encode(rawPassword: RawPassword): EncodedPassword = 
        EncodedPassword("encoded_${rawPassword.value}")
    
    override fun matches(rawPassword: RawPassword, encodedPassword: EncodedPassword): Boolean = 
        encodedPassword.value == "encoded_${rawPassword.value}"
}

class FakeMailSender : MailSender {
    var lastSentEmail: Email? = null
    var lastSentContent: MailContent? = null
    
    override fun send(to: Email, content: MailContent) {
        lastSentEmail = to
        lastSentContent = content
    }
}

class FakeCacheStore : CacheStore {
    private val cache = mutableMapOf<String, String>()
    
    override fun get(key: String): String? = cache[key]
    
    override fun put(key: String, value: String, ttl: Duration) {
        cache[key] = value
    }
    
    override fun delete(key: String) {
        cache.remove(key)
    }
    
    fun clear() {
        cache.clear()
    }
}