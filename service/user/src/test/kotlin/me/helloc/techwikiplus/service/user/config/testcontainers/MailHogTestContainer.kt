package me.helloc.techwikiplus.service.user.config.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * MailHog TestContainer 싱글톤 관리
 *
 * 테스트 실행 시 하나의 MailHog 컨테이너를 재사용하여 성능을 향상시킵니다.
 * 컨테이너는 JVM이 종료될 때까지 유지되며, 모든 통합 테스트에서 공유됩니다.
 *
 * MailHog는 개발 및 테스트 환경을 위한 이메일 테스트 도구로,
 * SMTP 서버 역할을 하며 발송된 이메일을 캡처하여 Web UI를 통해 확인할 수 있습니다.
 */
object MailHogTestContainer {
    private const val MAILHOG_IMAGE = "mailhog/mailhog:latest"
    private const val SMTP_PORT = 1025
    private const val WEB_PORT = 8025

    val instance: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse(MAILHOG_IMAGE))
            .withExposedPorts(SMTP_PORT, WEB_PORT)
            .withReuse(true)
            .withLabel("testcontainers.reuse.enable", "true")
            .apply {
                start()
                // JVM 종료 시 컨테이너 정리를 위한 shutdown hook 추가
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        try {
                            if (isRunning) {
                                stop()
                            }
                        } catch (e: Exception) {
                            // 종료 중 발생한 예외는 무시
                        }
                    },
                )
            }
    }

    /**
     * Spring Boot가 사용할 메일 서버 설정을 동적으로 제공
     */
    fun getProperties(): Map<String, String> =
        mapOf(
            "spring.mail.host" to instance.host,
            "spring.mail.port" to instance.getMappedPort(SMTP_PORT).toString(),
            "spring.mail.username" to "noreply@techwikiplus.com",
            "spring.mail.password" to "",
            "spring.mail.properties.mail.smtp.auth" to "false",
            "spring.mail.properties.mail.smtp.starttls.enable" to "false",
        )

    /**
     * MailHog Web UI URL을 반환
     * 테스트 디버깅 시 이 URL로 접속하여 발송된 이메일을 확인할 수 있습니다.
     */
    fun getWebUrl(): String = "http://${instance.host}:${instance.getMappedPort(WEB_PORT)}"
}
