# User Module - Claude AI Assistant Guide

## Project Overview

**user-module** is a Spring Boot 3.4.3-based reusable authentication and user management module designed to be
integrated into larger applications. It provides JWT-based authentication, OAuth2 (Google) login, user verification, and
role-based access control.

**Type**: Reusable Spring Boot Auto-Configuration Library
**Java Version**: 21
**Build Tool**: Maven
**Package**: com.comex.usermodule

## Development Environment

**Java:** This project uses **Java 21** managed via SDKMAN. Before running Maven commands, set:
`export JAVA_HOME=/Users/mihailoradovic/.sdkman/candidates/java/current`

## Technology Stack

### Core Framework

- **Spring Boot 3.4.3** - Main framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **Spring OAuth2 Client** - OAuth2 integration

### Database & Persistence

- **PostgreSQL** - Relational database option (default)
- **DynamoDB** - NoSQL database option (alternative)
- **Liquibase** - Database migration management (PostgreSQL only)
- **Hibernate Types 60** - Enhanced Hibernate type support (PostgreSQL only)
- **AWS DynamoDB SDK 2.30.2** - DynamoDB client and enhanced client

### Security & Authentication

- **JWT (jjwt 0.12.6)** - Token-based authentication
- **BCrypt** - Password encryption
- **OAuth2** - Google authentication integration

### External Services

- **AWS SES** - Email service (user verification)
- **AWS SNS** - SMS service
- **Thymeleaf** - Email template rendering

### Documentation & Testing

- **SpringDoc OpenAPI 2.8.1** - API documentation (Swagger)
- **JUnit 5** - Testing framework
- **Testcontainers 1.20.1** - Integration testing with PostgreSQL

### Utilities

- **Lombok 1.18.32** - Boilerplate code reduction
- **Resilience4j 2.3.0** - Retry mechanisms

## Architecture & Design Patterns

### Hexagonal Architecture (Ports & Adapters)

The project follows **Hexagonal Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                        Endpoint Layer                        │
│  (Controllers, Security, Web Models, Mappers)               │
│  - UserController                                            │
│  - JwtAuthFilter, OAuth2LoginSuccessHandler                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                         Core Layer                           │
│  (Domain, Services, Ports, Events)                          │
│  - User, Role (Domain Models)                               │
│  - UserService, UserAuthenticationService                   │
│  - UserRepository, EventPublisher (Ports/Interfaces)        │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    Infrastructure Layer                      │
│  (Persistence, Messaging, External Integrations)            │
│  - postgre/: UserPostgreRepository, UserEntity (PostgreSQL) │
│  - dynamodb/: UserDynamoRepository, UserDynamoEntity        │
│  - EventPublisherMock                                        │
└─────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. **Endpoint Layer** (`endpoint/`)

- REST API controllers
- Request/Response models
- Security configuration (JWT filters, OAuth2)
- Web-specific mappers
- **Entry point**: Controllers receive HTTP requests

#### 2. **Core Layer** (`core/`)

- **Domain**: Business entities (`User`, `Role`, `Permission`)
- **Service**: Business logic (`UserService`, `UserAuthenticationService`, `UserVerificationService`)
- **Port**: Interfaces for external dependencies (`UserRepository`, `EventPublisher`, `UserAuthenticator`)
- **DTO**: Data transfer objects
- **Event**: Domain events (`UserCreatedEvent`, `UserVerifiedEvent`)
- **Exception**: Business exceptions

#### 3. **Infrastructure Layer** (`infrastructure/`)

- **Persistence**: Database-specific implementations
    - **postgre/**: PostgreSQL JPA repositories, entities, mappers
    - **dynamodb/**: DynamoDB entities, mappers, repositories
- **Messaging**: Event publishing implementations
- External service integrations (AWS SES, SNS)

### Auto-Configuration Pattern

The module uses Spring Boot's **Auto-Configuration** mechanism to be easily integrated:

- `UserModuleAutoConfiguration` - Main auto-configuration entry point
- Provides conditional beans (`@ConditionalOnMissingBean`)
- Can be customized by consuming applications

## Directory Structure

```
src/main/java/com/comex/usermodule/
├── UserModuleApplication.java          # Main Spring Boot application
├── UserModuleAutoConfiguration.java    # Auto-configuration entry point
├── configuration/                      # Configuration classes
│   ├── SecurityConfiguration.java      # Security beans (JWT, Auth)
│   ├── UserConfiguration.java          # User module beans
│   ├── OAuth2GoogleConfiguration.java  # OAuth2 Google setup
│   └── UserProperties.java             # Configuration properties
├── core/                               # Core business logic (domain)
│   ├── domain/                         # Domain models
│   │   ├── User.java                   # User entity (implements UserDetails)
│   │   ├── Role.java                   # Role entity
│   │   └── UserStatus.java             # User status enum
│   ├── dto/                            # Data transfer objects
│   ├── event/                          # Domain events
│   ├── exception/                      # Business exceptions
│   ├── mapper/                         # Domain mappers
│   ├── port/                           # Interfaces (hexagonal ports)
│   │   ├── UserRepository.java
│   │   ├── EventPublisher.java
│   │   └── UserAuthenticator.java
│   └── service/                        # Business services
│       ├── UserService.java
│       ├── UserAuthenticationService.java
│       ├── UserVerificationService.java
│       └── JwtService.java
├── endpoint/                           # API layer (adapters)
│   ├── controller/                     # REST controllers
│   │   └── UserController.java
│   ├── model/                          # Request/Response models
│   ├── mapper/                         # Web mappers
│   └── security/                       # Security components
│       ├── jwt/JwtAuthFilter.java
│       ├── UserSpringAuthenticator.java
│       ├── UserGoogleSpringAuthenticator.java
│       └── OAuth2LoginSuccessHandler.java
└── infrastructure/                     # Infrastructure adapters
    ├── persistence/                    # Database access
    │   ├── postgre/                    # PostgreSQL implementations
    │   │   ├── entity/                 # JPA entities
    │   │   ├── jpa/                    # JPA repositories
    │   │   ├── mapper/                 # Entity mappers
    │   │   └── repository/             # Repository implementations
    │   └── dynamodb/                   # DynamoDB implementations
    │       ├── entity/                 # DynamoDB entities
    │       ├── mapper/                 # DynamoDB mappers
    │       └── repository/             # DynamoDB repositories
    └── messaging/                      # Event publishing
        └── EventPublisherMock.java

src/main/resources/
├── application.yml                     # Application configuration (PostgreSQL)
├── application-dynamodb.yml            # DynamoDB profile configuration
├── db/changelog/                       # Liquibase migrations (PostgreSQL only)
│   ├── user-master.yml
│   ├── 0_create-user-table.yml
│   ├── 1_create-role-and-permission-tables.yml
│   ├── insert-initial-roles.yml
│   └── insert-initial-users.yml
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Code Conventions & Style

### Naming Conventions

1. **Domain Models**: Simple names (`User`, `Role`, `Permission`)
2. **JPA Entities**: Suffixed with `Entity` (`UserEntity`, `RoleEntity`)
3. **DTOs**: Suffixed with `Dto` (`CreateUserDto`)
4. **Request Models**: Suffixed with `Request` (`CreateUserRequest`, `LoginUserRequest`)
5. **Response Models**: Suffixed with `Response` (`LoginTokenResponse`)
6. **Events**: Suffixed with `Event` (`UserCreatedEvent`, `UserVerifiedEvent`)
7. **Ports (Interfaces)**: Descriptive names (`UserRepository`, `EventPublisher`, `UserAuthenticator`)
8. **Repository Implementations**: Technology + `Repository` (`UserPostgreRepository`, `UserDynamoRepository`)
9. **JPA Repositories**: Technology + `JpaRepository` (`UserPostgreJpaRepository`)
10. **DynamoDB Entities**: Suffixed with `DynamoEntity` (`UserDynamoEntity`, `RoleDynamoEntity`)
11. **Mappers**: Subject + `Mapper` or Subject + Technology + `Mapper` (`UserMapper`, `UserEntityMapper`,
    `UserDynamoEntityMapper`, `UserWebMapper`)

### Package Organization

- **Separation by layer** (endpoint, core, infrastructure)
- **Separation by concern** within layers (domain, service, port, etc.)
- **Separation by technology** in infrastructure/persistence (postgre/, dynamodb/)
- Clear boundaries between layers

### Lombok Usage

The project heavily uses Lombok annotations:

- `@Data` - Domain models and DTOs
- `@Builder` - Complex object creation
- `@RequiredArgsConstructor` - Constructor injection
- `@Slf4j` - Logging
- `@AllArgsConstructor`, `@NoArgsConstructor` - JPA entities

### Logging

- Uses `@Slf4j` for logging
- Log levels:
    - `log.info()` - Important business operations (user creation)
    - `log.debug()` - Detailed flow information (repository operations)
- Structured log messages with context

### Exception Handling

- Custom exception: `UserException`
- Exception keys: `UserExceptionKey` enum
- Exceptions include context (e.g., email, verification code)

## Persistence Layer - PostgreSQL & DynamoDB

The user-module supports **two persistence implementations** that can be switched via configuration:

### Persistence Strategy Comparison

| Aspect            | PostgreSQL (Default)                         | DynamoDB                         |
|-------------------|----------------------------------------------|----------------------------------|
| **Activation**    | `user.persistence.type=postgresql` (default) | `user.persistence.type=dynamodb` |
| **Data Model**    | Normalized (relational)                      | Denormalized (document-based)    |
| **ID Type**       | Long (auto-generated sequence)               | String (UUID)                    |
| **Relationships** | Many-to-many tables                          | Embedded documents               |
| **Migrations**    | Liquibase                                    | Manual table creation            |
| **Queries**       | SQL/JPA                                      | Key-based + GSI                  |
| **Configuration** | UserConfiguration                            | DynamoDbConfiguration            |

### PostgreSQL Implementation (Default)

#### Migration Strategy

- **Tool**: Liquibase
- **Location**: `src/main/resources/db/changelog/`
- **Master file**: `user-master.yml`
- **Naming convention**: `{number}_{description}.yml`

#### Migration Files

1. `0_create-user-table.yml` - Users table
2. `1_create-role-and-permission-tables.yml` - Roles and permissions
3. `insert-initial-roles.yml` - Default roles
4. `insert-initial-users.yml` - Default users

#### Key Tables

- **user_table** - User accounts
- **role** - User roles
- **permission** - Role permissions
- **users_roles** - Many-to-many join table (user to roles)
- **roles_permissions** - Many-to-many join table (role to permissions)

#### Configuration

Activated by default or when `user.persistence.type=postgresql` in application.yml.

### DynamoDB Implementation

#### Table Structure

**Table Name**: `users` (configurable via `user.dynamodb.table-name`)

**Primary Key**: Email (partition key)

**Global Secondary Index**:

- **Index Name**: `verification-code-index`
- **Partition Key**: verificationCode

#### Data Model

Users are stored as single items with **embedded roles and permissions** (denormalized). Each user document contains email, id, username, password, status, createdAt, verificationCode, and nested roles array with permissions.

#### Configuration

Activated when `user.persistence.type=dynamodb` in application.yml or application-dynamodb.yml.

#### Table Creation

DynamoDB tables must be created manually before running the application. Use AWS CLI or CloudFormation:

```bash
aws dynamodb create-table \
  --table-name users \
  --attribute-definitions \
      AttributeName=email,AttributeType=S \
      AttributeName=verificationCode,AttributeType=S \
  --key-schema AttributeName=email,KeyType=HASH \
  --global-secondary-indexes \
      "[{\"IndexName\":\"verification-code-index\",
         \"KeySchema\":[{\"AttributeName\":\"verificationCode\",\"KeyType\":\"HASH\"}],
         \"Projection\":{\"ProjectionType\":\"ALL\"},
         \"ProvisionedThroughput\":{\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}}]" \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
```

#### Local DynamoDB Testing

For local development with DynamoDB Local, set the endpoint property to `http://localhost:8000` and run DynamoDB Local in Docker on port 8000.

#### Design Decisions

1. **Email as Partition Key**: Users are primarily accessed by email during login
2. **Denormalized Roles**: Roles and permissions embedded in user item to avoid additional queries
3. **GSI for Verification**: Enables efficient lookup by verification code
4. **UUID for ID**: Distributed ID generation without centralized sequences
5. **Role Cache**: In-memory cache of role definitions to avoid duplication
6. **List vs Set for Collections**: DynamoDB entities use `List` instead of `Set` for nested collections (`roles`, `permissions`) due to AWS SDK Enhanced Client limitations with `Set<@DynamoDbBean>`. The domain model still uses `Set<Role>` internally, and conversion happens in the mapper layer.

## API Endpoints

### User Controller (`/user`)

All endpoints require Bearer authentication except public ones.

| Method | Endpoint                   | Description       | Auth Required |
|--------|----------------------------|-------------------|---------------|
| POST   | `/user`                    | Create new user   | No            |
| POST   | `/user/login`              | User login        | No            |
| GET    | `/user/verify?code={code}` | Verify user email | No            |

### Security Configuration

- **JWT Authentication**: Bearer token in Authorization header
- **OAuth2 Google Login**: Configured at `/oauth2/authorization/google`
- **Public endpoints**: User creation, login, verification
- **Protected endpoints**: Secured with JWT filter

## Configuration Properties

### Application Configuration (`application.yml`)

Standard Spring Boot configuration including datasource, OAuth2 client registration for Google, Liquibase settings, and verification requirements.

### Custom Properties (`UserProperties`)

Configured via `@ConfigurationProperties` with prefix `user`:
- **verification-required**: Boolean flag for email verification
- **jwt.jwt-secret-key**: Secret key for JWT token generation
- **jwt.jwt-expiration**: Token expiration in milliseconds (default: 3600000 = 1 hour)
- **persistence.type**: Choose between `postgresql` (default) or `dynamodb`
- **dynamodb.table-name**: DynamoDB table name (default: users)
- **dynamodb.region**: AWS region for DynamoDB
- **dynamodb.endpoint**: Optional endpoint for local testing

## Development Guidelines

### When Adding New Features

1. **Start with Domain**: Define domain models in `core/domain/`
2. **Define Ports**: Create interfaces in `core/port/` for external dependencies
3. **Implement Services**: Add business logic in `core/service/`
4. **Create Adapters**: Implement ports in `infrastructure/` or `endpoint/`
5. **Expose API**: Add controllers in `endpoint/controller/`
6. **Add Tests**: Write unit and integration tests

### When Modifying Database

#### PostgreSQL:

1. Create new Liquibase changelog in `src/main/resources/db/changelog/`
2. Follow naming convention: `{number}_{description}.yml`
3. Add to master changelog (`user-master.yml`)
4. Update JPA entities in `infrastructure/persistence/postgre/entity/`
5. Update JPA repositories in `infrastructure/persistence/postgre/jpa/`
6. Update mappers in `infrastructure/persistence/postgre/mapper/`
7. Update domain models if needed

#### DynamoDB:

1. Update DynamoDB entities in `infrastructure/persistence/dynamodb/entity/`
2. Update table schema in AWS (add/modify attributes, indexes)
3. Update mappers in `infrastructure/persistence/dynamodb/mapper/`
4. Update domain models if needed

### Switching Between Persistence Implementations

To switch between PostgreSQL and DynamoDB:

1. **Update configuration** - Set `user.persistence.type` to `postgresql` or `dynamodb`
2. **Use Spring profiles** - Activate `application-dynamodb.yml` profile using `--spring.profiles.active=dynamodb`
3. **Ensure database is ready**:
    - PostgreSQL: Database and tables created via Liquibase
    - DynamoDB: Table and GSI created manually

**Note**: Data is NOT automatically migrated between databases. Migration must be handled separately if switching persistence strategies.

### When Adding Dependencies

- Add to `pom.xml` with proper versioning
- Use properties for version management
- Consider scope (compile, runtime, test, provided)

## Testing Strategy

The user-module follows strict testing conventions to ensure consistency, maintainability, and clarity. Tests are
organized by layer (core, infrastructure, endpoint) with specific rules for each.

### Test Structure

```
src/test/java/com/comex/usermodule/
├── core/
│   ├── helper/
│   │   └── UserTestInventory.java      # Test data factory
│   └── service/
│       ├── UserServiceTest.java
│       ├── UserAuthenticationServiceTest.java
│       ├── UserVerificationServiceTest.java
│       └── JwtServiceTest.java
├── infrastructure/
│   └── persistence/
│       ├── postgre/
│       │   ├── AbstractPostgresIntegrationTest.java    # Base class for PostgreSQL tests
│       │   ├── PostgresTestConfiguration.java          # Test configuration
│       │   └── UserPostgreRepositoryTest.java          # PostgreSQL integration tests (7 tests)
│       └── dynamodb/
│           ├── AbstractDynamoDbIntegrationTest.java    # Base class for DynamoDB tests
│           ├── DynamoDbTestConfiguration.java          # Test configuration
│           └── UserDynamoRepositoryTest.java           # DynamoDB integration tests (7 tests)
├── endpoint/
│   └── [Endpoint tests - to be covered]
└── UserModuleApplicationTests.java
```

### Testing Tools

- **JUnit 5** - Test framework with parameterized test support
- **Mockito** - Mocking framework for external dependencies (ports)
- **AssertJ** - Fluent assertion library
- **Spring Boot Test** - Integration testing support
- **Testcontainers** - PostgreSQL container for integration tests
- **MockMvc** - REST endpoint testing

### Global Testing Conventions

These rules apply to **ALL** tests in the project:

#### 1. **Naming Conventions**

**Rules:**

- Test class name: `{ClassName}Test`
- System under test variable: **ALWAYS** `sut`
- Test method name: `test{MethodName}` or `test{MethodName}{Scenario}`
- No `@DisplayName` annotations
- No `@Nested` classes

#### 2. **Test Structure**

Every test must follow the **Given-When-Then** pattern with clear comments:

**Rules:**

- Always use `// GIVEN`, `// WHEN`, `// THEN` section comments
- Given: Setup test data and mocks
- When: Execute the method under test
- Then: Verify results and interactions

#### 3. **Parameterized Tests**

Use `@ParameterizedTest` to reduce test duplication.

**When to use:**

- Testing the same behavior with different inputs
- Testing different scenarios that follow the same flow
- Reducing redundant test code

**Common annotations:**

- `@MethodSource("methodName")` - Custom parameter provider
- `@ValueSource(strings = {"value1", "value2"})` - Simple values
- `@NullAndEmptySource` - Null and empty string testing
- `@EnumSource` - Enum value testing

Use this only when it make sense. When test is same just with different parameters.

#### 4. **Test Data Management**

**Rules:**

- All test data comes from `UserTestInventory` in the `helper` package
- Use static imports for cleaner code: `import static com.comex.usermodule.core.helper.UserTestInventory.*;`
- Factory methods should have clear, descriptive names (e.g., `verifiedUser()`, `pendingUser()`, `userRole()`)
- Constants for commonly used values (e.g., `DEFAULT_EMAIL`, `DEFAULT_PASSWORD`)

#### 5. **Test Consolidation Principles**

**Guidelines:**

- Combine assertions for the same workflow into one test
- Each test should verify a distinct behavior or scenario
- Use parameterized tests for different inputs to the same behavior
- Aim for comprehensive tests over granular ones
- Avoid multiple tests testing the same behavior

### Core Module Testing

Core module tests focus on **business logic** in isolation from external dependencies.

#### Test Organization

```
src/test/java/com/comex/usermodule/core/
├── helper/
│   └── UserTestInventory.java          # Test data factory
└── service/
    ├── UserServiceTest.java            # User CRUD operations
    ├── UserAuthenticationServiceTest.java  # Authentication delegation
    ├── UserVerificationServiceTest.java    # Email verification
    └── JwtServiceTest.java              # JWT token operations
```

#### Mocking Strategy

**Core principle**: Only mock **ports** (external dependencies), use **real implementations** for internal utilities.

**What to mock:**

- ✅ Ports (interfaces in `core/port/`): `UserRepository`, `EventPublisher`, `UserAuthenticator`
- ✅ External configuration: `Boolean verificationRequired`
- ❌ Internal utilities: `UserMapper`
- ❌ Domain objects: `User`, `Role`

**Why?**

- Ports represent external system boundaries (database, messaging, authentication)
- Mappers and utilities are pure functions - test them with real implementations
- Testing with real mappers catches actual bugs (password encoding, field mapping)

**Setup pattern:**

Mock ports (UserRepository, EventPublisher) but use real implementations for utilities (UserMapper with BCryptPasswordEncoder). Initialize the system under test (sut) in @BeforeEach with required dependencies.

### Infrastructure Module Testing

Infrastructure module tests are **integration tests** that verify persistence layer implementations with real databases.

#### Test Organization

```
src/test/java/com/comex/usermodule/infrastructure/persistence/
├── postgre/
│   ├── AbstractPostgresIntegrationTest.java   # Base class for PostgreSQL tests
│   ├── PostgresTestConfiguration.java         # Test configuration
│   └── UserPostgreRepositoryTest.java         # PostgreSQL integration tests
└── dynamodb/
    ├── AbstractDynamoDbIntegrationTest.java   # Base class for DynamoDB tests
    └── UserDynamoRepositoryTest.java          # DynamoDB integration tests
```

#### Integration Test Characteristics

**Key Principles:**
- Lightweight Spring context with only persistence beans
- Real databases run in Docker containers via Testcontainers
- Tests verify actual database operations (save, query, update)
- No mocking - test the full persistence stack
- Each test is independent and can run in isolation

#### PostgreSQL Integration Tests

**Setup:**
- Uses `@DataJpaTest` for JPA slice testing
- Uses `PostgreSQLContainer` from Testcontainers
- Imports only `PostgresTestConfiguration` for minimal bean setup
- Liquibase migrations run automatically
- Much faster than full `@SpringBootTest`

**Base Class (AbstractPostgresIntegrationTest.java):**
- Uses `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@Import(PostgresTestConfiguration.class)`, `@Testcontainers`
- Configures PostgreSQL container (postgres:16-alpine)
- Uses `@DynamicPropertySource` to inject connection properties

**Test Class (UserPostgreRepositoryTest.java):**
- Extends AbstractPostgresIntegrationTest
- Autowires UserRepository (sut) and UserPostgreJpaRepository
- Uses `@AfterEach` to clean up data (deleteAll)
- Contains 7 tests for repository operations

#### DynamoDB Integration Tests

**Setup:**
- Uses `@ExtendWith(SpringExtension.class)` with minimal Spring context
- Uses `LocalStackContainer` from Testcontainers
- DynamoDB table created programmatically in `@BeforeAll`
- Only loads DynamoDB-specific beans (no OAuth2, Security, Web)

**Base Class (AbstractDynamoDbIntegrationTest.java):**
- Uses `@ExtendWith(SpringExtension.class)`, `@Testcontainers`, `@Import(DynamoDbTestConfiguration.class)`
- Configures LocalStack container for DynamoDB
- Uses `@DynamicPropertySource` to inject DynamoDB connection properties
- Uses `@BeforeAll` to create users table with GSI on verificationCode

**Test Class (UserDynamoRepositoryTest.java):**
- Extends AbstractDynamoDbIntegrationTest
- Autowires UserRepository (sut) and DynamoDbEnhancedClient
- Uses `@AfterEach` to clean up data (scan and delete all items)
- Contains 7 tests for repository operations

#### Test Configuration Files

**PostgresTestConfiguration.java:**
- `@TestConfiguration` class
- Provides beans: UserEntityMapper, UserRepository (UserPostgreRepository implementation)
- Minimal bean setup for JPA testing

**DynamoDbTestConfiguration.java:**
- `@TestConfiguration` class
- Provides beans: DynamoDbClient, DynamoDbEnhancedClient, roleDynamoCache (ConcurrentHashMap), UserDynamoEntityMapper, UserRepository (UserDynamoRepository implementation)
- Configures DynamoDB client with LocalStack credentials and endpoint

#### Infrastructure Testing Summary

| Test Class | Test Count | Container | Context Loading | Focus |
|------------|-----------|-----------|-----------------|-------|
| UserPostgreRepositoryTest | 7 | PostgreSQL | @DataJpaTest (lightweight) | PostgreSQL persistence operations |
| UserDynamoRepositoryTest | 7 | LocalStack DynamoDB | @ExtendWith(SpringExtension.class) (lightweight) | DynamoDB persistence operations |
| **Total** | **14** | **Testcontainers** | **Minimal Spring context** | **Full persistence stack** |

**Test Methods:**
1. `testSave` - Save user and verify fields
2. `testFindByEmail` - Retrieve user by email
3. `testFindByEmailOptionalWhenExists` - Optional email lookup when user exists
4. `testFindByEmailOptionalWhenEmpty` - Optional email lookup for non-existent/pending user
5. `testFindByEmailThrowsExceptionWhenNotFound` - Exception handling for missing user
6. `testFindByVerificationCode` - Verification code lookup
7. `testFindByVerificationCodeThrowsExceptionWhenNotFound` - Exception handling for invalid code

**Key Principles:**
- Integration tests use real databases in containers
- No mocking of persistence layer
- Test actual database operations and queries
- Both implementations tested with identical test scenarios
- Verifies consistency across different persistence strategies
- **Lightweight context loading** - Only persistence beans loaded, no OAuth2/Security/Web layers
- Uses `@AfterEach` cleanup to ensure test isolation

### Endpoint Testing

Testing strategies for the endpoint layer will be covered in future documentation updates.

### Running Tests

**Maven commands:**
- Run all tests: `./mvnw test`
- Run specific test class: `./mvnw test -Dtest=UserServiceTest`
- Run specific test method: `./mvnw test -Dtest=UserServiceTest#testCreateUser`
- Run tests with coverage: `./mvnw test jacoco:report`
- Run tests in a specific package: `./mvnw test -Dtest="com.comex.usermodule.core.service.*Test"`

## Build & Deployment

### Maven Commands

- Build and install locally: `./mvnw clean install`
- Build JAR: `./mvnw clean package`
- Run application: `./mvnw spring-boot:run`
- Update version: `./mvnw versions:set -DnewVersion=X.Y.Z`

### Deployment

- **Artifact Repository**: AWS CodeArtifact
- **Distribution**: Maven repository at com-comex-925199373191.d.codeartifact.us-east-2.amazonaws.com
- Configured in `pom.xml` under `<distributionManagement>`

### Docker Support

- Docker configuration available in `docker/` directory
- Can be containerized for microservices deployment

## Implementation Highlights

### Dual Persistence Support

The module demonstrates a clean implementation of **hexagonal architecture** by supporting multiple persistence
strategies:

1. **Single Port Interface** - `UserRepository` interface in `core/port/` defines contract
2. **Multiple Adapters** - `UserPostgreRepository` and `UserDynamoRepository` implement the same interface
3. **Conditional Configuration** - Spring's `@ConditionalOnProperty` activates appropriate implementation
4. **No Core Changes** - Business logic in `core/` remains unchanged regardless of persistence choice
5. **Configuration-Driven** - Switch persistence via simple property change
6. **Organized Structure** - PostgreSQL and DynamoDB implementations are cleanly separated in dedicated packages

This approach allows:

- Easy migration between databases
- Testing with different persistence strategies
- Adding new persistence implementations without modifying core logic
- Flexibility for different deployment environments (cloud vs on-premise)
- Clear separation of technology-specific code

### Package Structure

The infrastructure layer is organized by persistence technology:

```
infrastructure/persistence/
├── postgre/                    # PostgreSQL implementation
│   ├── entity/                 # UserEntity, RoleEntity, PermissionEntity
│   ├── jpa/                    # UserPostgreJpaRepository, RolePostgreJpaRepository
│   ├── mapper/                 # UserEntityMapper (JPA ↔ Domain)
│   └── repository/             # UserPostgreRepository (implements UserRepository)
└── dynamodb/                   # DynamoDB implementation
    ├── entity/                 # UserDynamoEntity, RoleDynamoEntity, PermissionDynamoEntity
    ├── mapper/                 # UserDynamoEntityMapper (DynamoDB ↔ Domain)
    └── repository/             # UserDynamoRepository (implements UserRepository)
```

This structure ensures:

- Technology-specific code is isolated
- No naming conflicts between implementations
- Easy to locate persistence-related code
- Simple to add additional persistence options (e.g., MongoDB, Cassandra)

## Additional Resources

- Spring Boot Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/
- Spring Security: https://docs.spring.io/spring-security/reference/
- Liquibase: https://docs.liquibase.com/
- JWT: https://jwt.io/
- AWS DynamoDB Developer Guide: https://docs.aws.amazon.com/dynamodb/
- DynamoDB Enhanced
  Client: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/dynamodb-enhanced-client.html
- DynamoDB Local: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html

---

**Last Updated**: October 2025
**Maintained By**: Comex Development Team
