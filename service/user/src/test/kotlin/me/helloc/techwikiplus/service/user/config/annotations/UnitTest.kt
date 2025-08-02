package me.helloc.techwikiplus.service.user.config.annotations

import org.junit.jupiter.api.Tag

/**
 * 단위 테스트 마커 어노테이션
 *
 * 단위 테스트를 식별하고 그룹화하기 위한 메타 어노테이션
 * CI/CD 파이프라인에서 빠른 피드백을 위해 단위 테스트만 실행할 때 사용
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("unit")
annotation class UnitTest
