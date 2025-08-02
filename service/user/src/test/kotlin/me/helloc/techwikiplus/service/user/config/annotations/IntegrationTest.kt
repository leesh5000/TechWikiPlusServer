package me.helloc.techwikiplus.service.user.config.annotations

import org.junit.jupiter.api.Tag

/**
 * 통합 테스트 마커 어노테이션
 *
 * 통합 테스트를 식별하고 그룹화하기 위한 메타 어노테이션
 * CI/CD 파이프라인에서 테스트를 선택적으로 실행할 때 사용
 *
 * @param generateDocs API 문서 생성 여부 (기본값: false)
 *                     true로 설정하면 테스트 실행 시 API 문서를 자동으로 생성합니다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration")
annotation class IntegrationTest(
    val generateDocs: Boolean = false,
)
