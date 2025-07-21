# Kotlin 코드 스타일 가이드

## 클래스 멤버 순서

모든 Kotlin 클래스는 다음 순서로 멤버를 정렬해야 합니다:

```kotlin
class ClassName {
    // 1. 상수 (companion object)
    companion object {
        private const val CONSTANT = "value"
    }
    
    // 2. 프로퍼티 선언
    // 2.1 공개 프로퍼티
    val publicProperty: String
    var publicMutableProperty: Int
    
    // 2.2 내부/보호 프로퍼티
    internal val internalProperty: String
    protected val protectedProperty: String
    
    // 2.3 비공개 프로퍼티
    private val privateProperty: String
    
    // 3. 초기화 블록
    init {
        // 초기화 로직
    }
    
    // 4. 보조 생성자
    constructor(param: String) : this() {
        // 생성자 로직
    }
    
    // 5. 오버라이드 메서드
    override fun equals(other: Any?): Boolean {
        // 구현
    }
    
    override fun hashCode(): Int {
        // 구현
    }
    
    override fun toString(): String {
        // 구현
    }
    
    // 6. 공개 메서드
    fun publicMethod() {
        // 메서드 구현
    }
    
    // 7. 내부/보호 메서드
    internal fun internalMethod() {
        // 메서드 구현
    }
    
    protected fun protectedMethod() {
        // 메서드 구현
    }
    
    // 8. 비공개 메서드
    private fun privateMethod() {
        // 메서드 구현
    }
}
```

## 정렬 원칙

1. **가시성 순서**: public → internal → protected → private
2. **종류별 그룹화**: 프로퍼티 → 생성자 → 오버라이드 → 일반 메서드
3. **논리적 그룹화**: 관련된 기능은 함께 배치

## IntelliJ IDEA 설정

1. Settings → Editor → Code Style → Kotlin
2. Arrangement 탭에서 위 순서대로 규칙 설정
3. Ctrl+Alt+L로 자동 정렬

## 적용 방법

1. 새 코드 작성 시 위 순서 준수
2. 기존 코드 수정 시 점진적으로 개선
3. 코드 리뷰에서 순서 확인

## 예외 사항

- 테스트 코드는 가독성을 우선으로 함
- 특별한 이유가 있는 경우 주석으로 명시
