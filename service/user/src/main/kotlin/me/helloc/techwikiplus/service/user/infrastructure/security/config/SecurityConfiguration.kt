package me.helloc.techwikiplus.service.user.infrastructure.security.config

import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.jwt.JwtAuthenticationEntryPoint
import me.helloc.techwikiplus.service.user.infrastructure.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    private val jwtTokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.disable()
            }
            .cors { cors ->
                cors.disable()  // API Gateway에서 CORS 처리
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트
                    .requestMatchers(
                        "/api/v1/users/signup",
                        "/api/v1/users/login",
                        "/api/v1/users/verify",
                        "/api/v1/users/verify/resend",
                        "/api/v1/users/refresh",
                    ).permitAll()
                    // Actuator 엔드포인트 (헬스체크 및 모니터링)
                    .requestMatchers(
                        "/actuator/**",
                    ).permitAll()
                    // Swagger/OpenAPI 문서
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                    ).permitAll()
                    // 정적 리소스
                    .requestMatchers(
                        "/static/**",
                        "/resources/**",
                    ).permitAll()
                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter {
        return JwtAuthenticationFilter(jwtTokenManager, userRepository, jwtAuthenticationEntryPoint)
    }

}
