# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :service:user:build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :service:user:test

# Run a single test class
./gradlew :service:user:test --tests "me.helloc.techwikiplus.user.domain.UserTest"

# Run a single test method
./gradlew :service:user:test --tests "me.helloc.techwikiplus.user.domain.UserTest.shouldCreateUserWithValidData"

# Build with kapt (for configuration metadata)
./gradlew :service:user:kaptKotlin
```

### Code Quality
```bash
# Run ktlint check
./gradlew ktlintCheck

# Auto-format code with ktlint
./gradlew ktlintFormat

# Check and format specific module
./gradlew :service:user:ktlintCheck
./gradlew :service:user:ktlintFormat
```

### Local Development
```bash
# Start infrastructure (MySQL, Redis) using docker-compose
cd service/user
docker-compose -f docker-compose-infra.yml up -d

# Run the User Service
./gradlew :service:user:bootRun

# The service will start on port 9000
```

## Architecture Overview

### Multi-Module Structure
The project follows a multi-module Gradle structure:

- **common/snowflake**: Distributed ID generation using Twitter's Snowflake algorithm
- **service/user**: User authentication and management microservice

### Domain-Driven Design (DDD)
Each service follows DDD principles with clear layer separation:

- **domain**: Core business logic, entities, value objects, domain services
- **application**: Use cases orchestrating domain logic
- **infrastructure**: Technical implementations (persistence, security, external services)
- **interfaces**: API controllers and DTOs

### Key Architectural Patterns

1. **Hexagonal Architecture**: Business logic is isolated from external concerns
   - Domain layer has no dependencies on infrastructure
   - Infrastructure implements domain interfaces (ports)
   - Application layer orchestrates domain services

2. **Repository Pattern**: Abstract data access through interfaces
   - Domain defines repository interfaces
   - Infrastructure provides JPA implementations

3. **Dependency Injection**: Spring manages component wiring
   - Constructor injection preferred
   - Interfaces in domain, implementations in infrastructure

### Testing Strategy

1. **Unit Tests**: Pure domain logic testing with fakes
   - Located in `domain` test packages
   - Use fake implementations for external dependencies
   - Fast execution, no Spring context

2. **Integration Tests**: Infrastructure layer testing
   - Use `IntegrationTestSupport` base class
   - TestContainers for MySQL and Redis
   - Tests actual database interactions

### Security Architecture

- JWT-based authentication with access/refresh tokens
- Spring Security for request filtering
- BCrypt for password hashing
- Redis for temporary data (verification codes)

### Code Style

Follow the Kotlin style guide in `docs/kotlin-style-guide.md`:
- Consistent member ordering in classes
- Visibility modifiers: public → internal → protected → private
- Use ktlint for automatic formatting

### Environment Configuration

- Use `application.yml` for base configuration
- Override with environment variables for sensitive data:
  - `JWT_SECRET`
  - `MAIL_USERNAME` / `MAIL_PASSWORD`
  - `REDIS_PASSWORD`
  
#### Mail Configuration
- `spring.mail.type`: Choose mail sender implementation
  - `smtp`: Real email sending via SMTP (production)
  - `console`: Log to console without sending (development/test)
- Default ports changed: 587 → 1025 for local development

### Database Setup

- MySQL 8.0+ on port 13306 (local development)
- Redis on port 16379 (local development)
- Database name: `techwikiplus`
- Flyway migrations in `resources/db/schema/`
