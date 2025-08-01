package me.helloc.techwikiplus.service.user.infrastructure.di.spring

import me.helloc.techwikiplus.domain.service.port.UserRepository
import me.helloc.techwikiplus.infrastructure.persistence.UserRepositoryImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InfrastructureBeanConfiguration {

    @Bean
    fun userRepository(): UserRepository {
        return UserRepositoryImpl()
    }
}
