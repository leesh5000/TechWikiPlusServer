package me.helloc.techwikiplus.service.apigateway.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class RouteConfigTest(
    private val routeLocator: RouteLocator,
) : FunSpec({

        test("Should have user service route configured") {
            val routes = routeLocator.routes.collectList().block()
            routes shouldNotBe null

            val userServiceRoute = routes?.find { it.id == "user-service-route" }
            userServiceRoute shouldNotBe null
            userServiceRoute?.uri?.toString() shouldBe "http://localhost:9000"
        }

        test("Should have document service route configured") {
            val routes = routeLocator.routes.collectList().block()
            routes shouldNotBe null

            val documentServiceRoute = routes?.find { it.id == "document-service-route" }
            documentServiceRoute shouldNotBe null
            documentServiceRoute?.uri?.toString() shouldBe "http://localhost:9001"
        }

        test("Should have health check route configured") {
            val routes = routeLocator.routes.collectList().block()
            routes shouldNotBe null

            val healthCheckRoute = routes?.find { it.id == "health-check-route" }
            healthCheckRoute shouldNotBe null
            healthCheckRoute?.uri?.toString() shouldBe "http://localhost:8080/actuator/health"
        }
    })
