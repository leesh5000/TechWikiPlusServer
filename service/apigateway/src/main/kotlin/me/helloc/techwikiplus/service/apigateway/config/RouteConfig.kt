package me.helloc.techwikiplus.service.apigateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class RouteConfig {
    @Bean
    fun customRoutes(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("user-service-route") { r ->
                r.path("/api/v1/users/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway-Request-Id", UUID.randomUUID().toString())
                            .addRequestHeader("X-Service-Name", "user-service")
                            .addResponseHeader("X-Gateway-Response", "api-gateway")
                    }
                    .uri("http://localhost:9000")
            }
            .route("document-service-route") { r ->
                r.path("/api/v1/documents/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway-Request-Id", UUID.randomUUID().toString())
                            .addRequestHeader("X-Service-Name", "document-service")
                            .addResponseHeader("X-Gateway-Response", "api-gateway")
                    }
                    .uri("http://localhost:9001")
            }
            .route("health-check-route") { r ->
                r.path("/health")
                    .filters { f ->
                        f.addResponseHeader("X-Gateway-Health", "ok")
                    }
                    .uri("http://localhost:8080/actuator/health")
            }
            .build()
    }
}
