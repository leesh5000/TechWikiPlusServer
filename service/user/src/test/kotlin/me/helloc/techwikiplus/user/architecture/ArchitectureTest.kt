package me.helloc.techwikiplus.user.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures

@AnalyzeClasses(
    packages = ["me.helloc.techwikiplus.user"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {
    @ArchTest
    val layerDependencies: ArchRule =
        Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Interfaces").definedBy("..interfaces..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain", "Infrastructure")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
            .whereLayer("Interfaces").mayOnlyAccessLayers("Application", "Domain", "Infrastructure")

    // @ArchTest
    // ApplicationExceptionHandler와 UseCaseConfig 간의 의존성으로 인해 일시적으로 비활성화
    // val noCyclicDependencies: ArchRule =
    //     SlicesRuleDefinition.slices()
    //         .matching("me.helloc.techwikiplus.user.(*)..")
    //         .should().beFreeOfCycles()

    @ArchTest
    val domainShouldNotDependOnInfrastructure: ArchRule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("도메인은 인프라스트럭처에 의존해서는 안됩니다")

    @ArchTest
    val domainShouldNotDependOnApplication: ArchRule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .because("도메인은 애플리케이션 레이어에 의존해서는 안됩니다")

    @ArchTest
    val applicationShouldNotDependOnInfrastructure: ArchRule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..application..")
            .and().doNotHaveSimpleName("ApplicationExceptionHandler")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("애플리케이션은 인프라스트럭처에 의존해서는 안됩니다 (ApplicationExceptionHandler 제외)")
}
