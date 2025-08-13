package me.helloc.techwikiplus.service.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("아키텍처 테스트")
class ArchitectureTest {
    private lateinit var importedClasses: JavaClasses

    @BeforeEach
    fun setup() {
        importedClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("me.helloc.techwikiplus.service")
    }

    @Nested
    @DisplayName("패키지 의존성 규칙")
    inner class PackageDependencyRules {
        @Test
        @DisplayName("common 패키지는 다른 모든 패키지를 참조할 수 있다")
        fun commonPackageCanAccessAllPackages() {
            val rule =
                classes()
                    .that().resideInAPackage("..service.common..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                        "..service.common..",
                        "..service.user..",
                        "..service.document..",
                        "..service..",
                        "java..",
                        "kotlin..",
                        "org.springframework..",
                        "com.fasterxml..",
                        "org.slf4j..",
                        "javax..",
                        "jakarta..",
                    )

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("user 패키지는 document 패키지를 참조할 수 없다")
        fun userPackageCannotAccessDocumentPackage() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..service.user..")
                    .should().accessClassesThat()
                    .resideInAPackage("..service.document..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("document 패키지는 user 패키지를 참조할 수 없다")
        fun documentPackageCannotAccessUserPackage() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..service.document..")
                    .should().accessClassesThat()
                    .resideInAPackage("..service.user..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("도메인 패키지는 자기 자신과 common 패키지만 참조할 수 있다")
        fun domainPackagesCanOnlyAccessSelfAndCommon() {
            val domainPackages = listOf("user", "document")

            domainPackages.forEach { domainPackage ->
                val otherDomains = domainPackages.filter { it != domainPackage }

                otherDomains.forEach { otherDomain ->
                    val rule =
                        noClasses()
                            .that().resideInAPackage("..service.$domainPackage..")
                            .should().accessClassesThat()
                            .resideInAPackage("..service.$otherDomain..")

                    rule.check(importedClasses)
                }
            }
        }
    }

    @Nested
    @DisplayName("의존 방향 규칙")
    inner class LayeredArchitectureRules {
        @Test
        @DisplayName("domain 레이어는 외부 의존성을 가질 수 없다")
        fun domainLayerShouldNotHaveExternalDependencies() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().doNotHaveFullyQualifiedName("..domain.exception..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                        "..interfaces..",
                        "..application..",
                        "..infrastructure..",
                        "org.springframework..",
                        "javax.persistence..",
                        "jakarta.persistence..",
                    )

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("application 레이어는 interfaces 레이어의 컨트롤러를 직접 참조할 수 없다")
        fun applicationLayerShouldNotAccessInterfacesController() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..application..")
                    .should().accessClassesThat()
                    .resideInAPackage("..interfaces.web.controller..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("interfaces 레이어는 infrastructure를 직접 참조할 수 없다")
        fun interfacesLayerShouldNotDirectlyAccessInfrastructure() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..interfaces..")
                    .and().doNotHaveFullyQualifiedName("..interfaces.web..")
                    .should().accessClassesThat()
                    .resideInAPackage("..infrastructure..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("infrastructure 레이어는 다른 레이어에서 참조될 수 없다")
        fun infrastructureLayerShouldNotBeAccessedByOtherLayers() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .or().resideInAPackage("..application..")
                    .should().accessClassesThat()
                    .resideInAPackage("..infrastructure..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("domain 레이어는 다른 레이어를 참조할 수 없다")
        fun domainLayerShouldNotAccessOtherLayers() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                        "..application..",
                        "..interfaces..",
                        "..infrastructure..",
                    )

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("도메인 모델 규칙")
    inner class DomainModelRules {
        @Test
        @DisplayName("도메인 모델은 불변이어야 한다")
        fun domainModelsShouldBeImmutable() {
            val rule =
                classes()
                    .that().resideInAPackage("..domain.model..")
                    .and().areNotEnums()
                    .and().doNotHaveSimpleName("Companion")
                    .should().haveOnlyFinalFields()

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("도메인 exception은 도메인 패키지 내에 있어야 한다")
        fun domainExceptionsShouldBeInDomainPackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("DomainException")
                    .or().haveSimpleNameEndingWith("ErrorCode")
                    .should().resideInAPackage("..domain.exception..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("컨트롤러 규칙")
    inner class ControllerRules {
        @Test
        @DisplayName("컨트롤러는 interfaces.web 패키지에 있어야 한다")
        fun controllersShouldBeInInterfacesWebPackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                    .should().resideInAPackage("..interfaces.web..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("컨트롤러는 도메인 서비스를 직접 호출할 수 없다")
        fun controllersShouldNotDirectlyCallDomainServices() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..interfaces.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().accessClassesThat()
                    .resideInAPackage("..domain.service..")
                    .andShould().notBeInterfaces()

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("서비스 규칙")
    inner class ServiceRules {
        @Test
        @DisplayName("도메인 서비스는 domain.service 패키지에 있어야 한다")
        fun domainServicesShouldBeInDomainServicePackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.service..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("애플리케이션 서비스(Facade)는 application.facade 패키지에 있어야 한다")
        fun applicationServicesShouldBeInApplicationServicePackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Facade")
                    .should().resideInAPackage("..application.facade..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("포트와 어댑터 규칙")
    inner class PortsAndAdaptersRules {
        @Test
        @DisplayName("포트 인터페이스는 port 패키지에 있어야 한다")
        fun portInterfacesShouldBeInPortPackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Port")
                    .or().haveSimpleNameEndingWith("UseCase")
                    .and().areInterfaces()
                    .should().resideInAPackage("..port..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("인프라스트럭처 구현체는 infrastructure 패키지에 있어야 한다")
        fun infrastructureImplementationsShouldBeInInfrastructurePackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Impl")
                    .or().haveSimpleNameEndingWith("Adapter")
                    .and().areNotInterfaces()
                    .and().resideInAPackage("..common..")
                    .should().resideInAPackage("..infrastructure..")

            rule.check(importedClasses)
        }
    }
}
