import me.helloc.techwikiplus.service.document.domain.model.Title
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException

fun main() {
    println("Testing Title class with control characters...")
    
    // Test 1: Regular spaces should work
    try {
        val title1 = Title("Spring Boot")
        println("✓ Title with regular space: '${title1.value}' - PASSED")
    } catch (e: Exception) {
        println("✗ Title with regular space failed: ${e.message}")
    }
    
    // Test 2: Multiple spaces should work
    try {
        val title2 = Title("Spring  Boot  Guide")
        println("✓ Title with multiple spaces: '${title2.value}' - PASSED")
    } catch (e: Exception) {
        println("✗ Title with multiple spaces failed: ${e.message}")
    }
    
    // Test 3: Newline should fail
    try {
        val title3 = Title("Spring\nBoot")
        println("✗ Title with newline should have failed but didn't")
    } catch (e: DocumentDomainException) {
        println("✓ Title with newline correctly rejected - PASSED")
    }
    
    // Test 4: Tab should fail
    try {
        val title4 = Title("Spring\tBoot")
        println("✗ Title with tab should have failed but didn't")
    } catch (e: DocumentDomainException) {
        println("✓ Title with tab correctly rejected - PASSED")
    }
    
    // Test 5: Carriage return should fail
    try {
        val title5 = Title("Spring\rBoot")
        println("✗ Title with carriage return should have failed but didn't")
    } catch (e: DocumentDomainException) {
        println("✓ Title with carriage return correctly rejected - PASSED")
    }
    
    println("\nAll tests completed!")
}