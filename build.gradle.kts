plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    // ktlint: Kotlin 코드 스타일 검사 및 포맷팅 도구
    // - ./gradlew ktlintCheck: 코드 스타일 위반 검사
    // - ./gradlew ktlintFormat: 자동 코드 포맷팅
    // - ./gradlew addKtlintCheckGitPreCommitHook: Git 커밋 전 자동 검사 설정
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "com.helloc.techwikiplus"
version = "0.0.2"

allprojects {

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.plugin.jpa")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        // kotest
        testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
        testImplementation("io.kotest:kotest-assertions-core:5.7.2")
        testImplementation("io.kotest:kotest-property:5.7.2")
        testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")

        // ArchUnit: 아키텍처 테스트
        testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// ktlint 설정
ktlint {
    version.set("1.0.1")
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Git pre-commit hook 설치 태스크
tasks.register<Exec>("installGitHook") {
    description = "Install pre-commit git hook for ktlint"
    group = "git hooks"
    commandLine("sh", "-c", "./gradlew addKtlintCheckGitPreCommitHook")
}
