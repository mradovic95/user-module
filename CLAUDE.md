# User Module - Claude AI Assistant Guide

## Project Overview

**user-module** is a Spring Boot 3.4.3-based reusable authentication and user management module designed to be
integrated into larger applications. It provides JWT-based authentication, OAuth2 (Google) login, user verification, and
role-based access control.

**Type**: Multi-Module Maven Project - Reusable Spring Boot Auto-Configuration Library
**Java Version**: 21
**Build Tool**: Maven (Multi-Module)
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

## Project Structure - Multi-Module Maven

The user-module is organized as a **multi-module Maven project** with 7 distinct modules, each serving a specific purpose:

```
user-module (parent pom)
├── user-module-core                      # Domain models, services, ports
├── user-module-infrastructure-postgre    # PostgreSQL persistence adapter
├── user-module-infrastructure-dynamodb   # DynamoDB persistence adapter
├── user-module-endpoint                  # REST controllers and web models
├── user-module-configuration             # Auto-configuration and security
├── user-module-starter-postgre           # PostgreSQL starter dependency
└── user-module-starter-dynamodb          # DynamoDB starter dependency
```

## Architecture & Design Patterns

### Hexagonal Architecture (Ports & Adapters)

The project follows **Hexagonal Architecture** principles with clear separation of concerns across modules:

```
┌────────────────────────────────────────────────────────────────────┐
│                    Starter Modules (Entry Point)                    │
│   user-module-starter-postgre | user-module-starter-dynamodb       │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼────────┐   ┌───────────▼───────────┐   ┌───────▼──────────┐
│  Endpoint      │   │   Configuration        │   │  Infrastructure  │
│  Module        │   │   Module               │   │  Modules         │
│                │   │                        │   │                  │
│ UserController │   │ SecurityConfiguration  │   │ PostgreSQL or    │
│ Web Models     │   │ JwtAuthFilter          │   │ DynamoDB         │
│ UserWebMapper  │   │ OAuth2 Config          │   │ Implementation   │
└───────┬────────┘   └───────────┬────────────┘   └───────┬──────────┘
        │                        │                        │
        └────────────────────────┼────────────────────────┘
                                 │
                        ┌────────▼────────┐
                        │   Core Module   │
                        │                 │
                        │ Domain Models   │
                        │ Services        │
                        │ Ports           │
                        │ Events          │
                        └─────────────────┘
```

### Module Responsibilities

#### 1. **Starter Modules** (`user-module-starter-*`)

**Purpose**: Convenience dependencies that aggregate all required modules for a specific persistence strategy.

- **user-module-starter-postgre**: Includes configuration, endpoint, and PostgreSQL infrastructure
- **user-module-starter-dynamodb**: Includes configuration, endpoint, and DynamoDB infrastructure
- **Usage**: Applications only need to depend on one starter module
- **Auto-configuration**: Automatically imports the appropriate UserModuleAutoConfiguration

#### 2. **Core Module** (`user-module-core`)

**Purpose**: Technology-agnostic business logic and domain models. This module has **zero** Spring dependencies (except for testing).

- **Domain**: Business entities (`User`, `Role`, `Permission`)
- **Service**: Business logic (`UserService`, `UserAuthenticationService`, `UserVerificationService`, `JwtService`)
- **Port**: Interfaces for external dependencies (`UserRepository`, `EventPublisher`, `UserAuthenticator`, `PasswordEncoder`)
- **DTO**: Data transfer objects (`CreateUserDto`, `LoginUserDto`)
- **Event**: Domain events (`UserCreatedEvent`, `UserVerifiedEvent`)
- **Exception**: Business exceptions (`UserException`, `UserExceptionKey`)
- **Mapper**: Domain object mappers (`UserMapper`)

#### 3. **Infrastructure Modules** (`user-module-infrastructure-*`)

**Purpose**: Implement core port interfaces using specific persistence technologies.

**PostgreSQL Module** (`user-module-infrastructure-postgre`):
- PostgreSQL JPA repositories (`UserPostgreJpaRepository`, `RolePostgreJpaRepository`)
- JPA entities (`UserEntity`, `RoleEntity`, `PermissionEntity`)
- Repository implementation (`UserPostgreRepository` implements `UserRepository`)
- Entity mappers (`UserEntityMapper`)
- Liquibase migrations in `src/main/resources/db/changelog/`

**DynamoDB Module** (`user-module-infrastructure-dynamodb`):
- DynamoDB entities (`UserDynamoEntity`, `RoleDynamoEntity`, `PermissionDynamoEntity`)
- Repository implementation (`UserDynamoRepository` implements `UserRepository`)
- Entity mappers (`UserDynamoEntityMapper`)
- DynamoDB Enhanced Client configuration

#### 4. **Endpoint Module** (`user-module-endpoint`)

**Purpose**: REST API layer with controllers and web-specific models.

- REST API controllers (`UserController`)
- Request models (`CreateUserRequest`, `LoginUserRequest`)
- Response models (`UserResponse`, `LoginTokenResponse`)
- Web mappers (`UserWebMapper` - converts between domain and web models)
- **Note**: This module is technology-agnostic and doesn't depend on Spring Security (that's in configuration module)

#### 5. **Configuration Module** (`user-module-configuration`)

**Purpose**: Auto-configuration, security setup, and bean wiring.

- Auto-configuration classes (`UserConfiguration`)
- Spring Security configuration (`SecurityConfiguration`)
- JWT authentication filter (`JwtAuthFilter`)
- OAuth2 configuration (`OAuth2GoogleConfiguration`, `OAuth2LoginSuccessHandler`)
- Security adapters (`UserGoogleSpringAuthenticator` - implements core ports)
- Configuration properties (`UserProperties`)
- Bean definitions and conditional configurations

### Auto-Configuration Pattern

The module uses Spring Boot's **Auto-Configuration** mechanism to be easily integrated:

- `UserModuleAutoConfiguration` - Main auto-configuration entry point
- Provides conditional beans (`@ConditionalOnMissingBean`)
- Can be customized by consuming applications

## Directory Structure - Multi-Module Maven

```
user-module/                                    # Parent Maven module (aggregator)
├── pom.xml                                     # Parent POM with module definitions
│
├── user-module-core/                           # Core domain module
│   ├── pom.xml
│   └── src/main/java/com/comex/usermodule/core/
│       ├── domain/                             # Domain models
│       │   ├── User.java
│       │   ├── Role.java
│       │   └── UserStatus.java
│       ├── dto/                                # Data transfer objects
│       │   ├── CreateUserDto.java
│       │   └── LoginUserDto.java
│       ├── event/                              # Domain events
│       │   ├── UserCreatedEvent.java
│       │   └── UserVerifiedEvent.java
│       ├── exception/                          # Business exceptions
│       │   ├── UserException.java
│       │   └── UserExceptionKey.java
│       ├── mapper/                             # Domain mappers
│       │   └── UserMapper.java
│       ├── port/                               # Port interfaces (hexagonal)
│       │   ├── UserRepository.java
│       │   ├── EventPublisher.java
│       │   ├── UserAuthenticator.java
│       │   └── PasswordEncoder.java
│       └── service/                            # Business services
│           ├── UserService.java
│           ├── UserAuthenticationService.java
│           ├── UserVerificationService.java
│           └── JwtService.java
│
├── user-module-infrastructure-postgre/         # PostgreSQL persistence
│   ├── pom.xml
│   ├── src/main/java/com/comex/usermodule/infrastructure/persistence/postgre/
│   │   ├── entity/                             # JPA entities
│   │   │   ├── UserEntity.java
│   │   │   ├── RoleEntity.java
│   │   │   └── PermissionEntity.java
│   │   ├── jpa/                                # JPA repositories
│   │   │   ├── UserPostgreJpaRepository.java
│   │   │   └── RolePostgreJpaRepository.java
│   │   ├── mapper/                             # Entity mappers
│   │   │   └── UserEntityMapper.java
│   │   └── repository/                         # Port implementations
│   │       └── UserPostgreRepository.java
│   └── src/main/resources/db/changelog/        # Liquibase migrations
│       ├── user-master.yml
│       ├── 0_create-user-table.yml
│       ├── 1_create-role-and-permission-tables.yml
│       ├── insert-initial-roles.yml
│       └── insert-initial-users.yml
│
├── user-module-infrastructure-dynamodb/        # DynamoDB persistence
│   ├── pom.xml
│   └── src/main/java/com/comex/usermodule/infrastructure/persistence/dynamodb/
│       ├── entity/                             # DynamoDB entities
│       │   ├── UserDynamoEntity.java
│       │   ├── RoleDynamoEntity.java
│       │   └── PermissionDynamoEntity.java
│       ├── mapper/                             # Entity mappers
│       │   └── UserDynamoEntityMapper.java
│       └── repository/                         # Port implementations
│           └── UserDynamoRepository.java
│
├── user-module-endpoint/                       # REST API endpoints
│   ├── pom.xml
│   └── src/main/java/com/comex/usermodule/endpoint/
│       ├── controller/                         # REST controllers
│       │   └── UserController.java
│       ├── model/                              # Request/Response models
│       │   ├── CreateUserRequest.java
│       │   ├── LoginUserRequest.java
│       │   ├── UserResponse.java
│       │   └── LoginTokenResponse.java
│       └── mapper/                             # Web mappers
│           └── UserWebMapper.java
│
├── user-module-configuration/                  # Auto-configuration & security
│   ├── pom.xml
│   └── src/main/java/com/comex/usermodule/
│       ├── configuration/                      # Configuration classes
│       │   ├── UserConfiguration.java
│       │   ├── SecurityConfiguration.java
│       │   ├── OAuth2GoogleConfiguration.java
│       │   └── UserProperties.java
│       └── security/                           # Security components
│           ├── jwt/JwtAuthFilter.java
│           ├── UserGoogleSpringAuthenticator.java
│           └── OAuth2LoginSuccessHandler.java
│
├── user-module-starter-postgre/               # PostgreSQL starter
│   ├── pom.xml                                 # Aggregates: configuration, endpoint, infra-postgre
│   └── src/main/java/com/comex/usermodule/starter/postgre/
│       ├── UserModuleAutoConfiguration.java
│       └── configuration/
│           └── UserPostgreRepositoryConfiguration.java
│
└── user-module-starter-dynamodb/              # DynamoDB starter
    ├── pom.xml                                 # Aggregates: configuration, endpoint, infra-dynamodb
    └── src/main/java/com/comex/usermodule/starter/dynamodb/
        ├── UserModuleAutoConfiguration.java
        └── configuration/
            └── UserDynamoRepositoryConfiguration.java
```

### Module Structure Benefits

1. **Clear Separation**: Each module has a single, well-defined responsibility
2. **Dependency Management**: Core module has no Spring dependencies, making business logic portable
3. **Flexible Integration**: Applications choose PostgreSQL or DynamoDB starter based on needs
4. **Independent Testing**: Each module can be tested in isolation
5. **Reusability**: Modules can be mixed and matched for different use cases

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

- **Separation by module** - Each Maven module is a separate deliverable artifact
- **Separation by layer** - Core, endpoint, infrastructure, and configuration are separate modules
- **Separation by concern** within modules (domain, service, port, etc. within core module)
- **Separation by technology** - PostgreSQL and DynamoDB are separate infrastructure modules
- **Clear module boundaries** - Dependencies flow in one direction (no circular dependencies)
- **Module independence** - Each module can be tested and deployed independently

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

1. **Start with Domain** (in `user-module-core`): Define domain models in `core/domain/`
2. **Define Ports** (in `user-module-core`): Create interfaces in `core/port/` for external dependencies
3. **Implement Services** (in `user-module-core`): Add business logic in `core/service/`
4. **Create Adapters**:
   - Infrastructure: Implement ports in `user-module-infrastructure-*` modules
   - Endpoint: Add web layer code in `user-module-endpoint`
5. **Expose API** (in `user-module-endpoint`): Add controllers in `endpoint/controller/`
6. **Configure** (in `user-module-configuration`): Wire beans and add security if needed
7. **Add Tests**: Write tests in each module (see Testing Strategy below)

### When Modifying Database

#### PostgreSQL (user-module-infrastructure-postgre):

1. Create new Liquibase changelog in `user-module-infrastructure-postgre/src/main/resources/db/changelog/`
2. Follow naming convention: `{number}_{description}.yml`
3. Add to master changelog (`user-master.yml`)
4. Update JPA entities in `user-module-infrastructure-postgre/.../entity/`
5. Update JPA repositories in `user-module-infrastructure-postgre/.../jpa/`
6. Update mappers in `user-module-infrastructure-postgre/.../mapper/`
7. Update domain models in `user-module-core` if needed

#### DynamoDB (user-module-infrastructure-dynamodb):

1. Update DynamoDB entities in `user-module-infrastructure-dynamodb/.../entity/`
2. Update table schema in AWS (add/modify attributes, indexes)
3. Update mappers in `user-module-infrastructure-dynamodb/.../mapper/`
4. Update domain models in `user-module-core` if needed

### Choosing Between PostgreSQL and DynamoDB

Applications choose persistence strategy by selecting the appropriate **starter module**:

**Option 1: PostgreSQL**
```xml
<dependency>
    <groupId>com.comex</groupId>
    <artifactId>user-module-starter-postgre</artifactId>
    <version>0.0.6-SNAPSHOT</version>
</dependency>
```

**Option 2: DynamoDB**
```xml
<dependency>
    <groupId>com.comex</groupId>
    <artifactId>user-module-starter-dynamodb</artifactId>
    <version>0.0.6-SNAPSHOT</version>
</dependency>
```

The starter module automatically includes the correct infrastructure module and configuration.

**Note**: Data is NOT automatically migrated between databases. Migration must be handled separately if switching persistence strategies.

### When Adding Module Dependencies

**In Parent POM** (`user-module/pom.xml`):
- Add version properties for external dependencies
- Add dependencies to `<dependencyManagement>` section

**In Module POM** (e.g., `user-module-core/pom.xml`):
- Reference dependencies without version (inherited from parent)
- Add module-to-module dependencies in `<dependencies>` section
- Respect dependency flow: `starter → configuration/endpoint/infrastructure → core`

## Testing Strategy

The user-module follows strict testing conventions to ensure consistency, maintainability, and clarity. Tests are organized **by Maven module**, with each module containing its own tests.

### Test Structure - Multi-Module

### Test-Jar Artifact (user-module-core)

The **user-module-core** module exports its test classes as a **test-jar** artifact to allow other modules to reuse test data:

**Configured in** `user-module-core/pom.xml`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>test-jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Used in other modules** (e.g., `user-module-endpoint/pom.xml`):
```xml
<dependency>
    <groupId>com.comex</groupId>
    <artifactId>user-module-core</artifactId>
    <version>${project.version}</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

This allows endpoint and infrastructure tests to use `UserTestInventory` for creating test data.

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

Testing strategies for the **user-module-endpoint** module with `@WebMvcTest`.

#### Test Organization

```
user-module-endpoint/src/test/java/com/comex/usermodule/endpoint/
├── EndpointTestConfiguration.java      # @SpringBootConfiguration for @WebMvcTest
├── helper/
│   └── EndpointTestInventory.java      # Web-specific test data factory
└── controller/
    └── UserControllerTest.java         # Controller tests with MockMvc
```

#### Endpoint Test Characteristics

**Key Principles:**
- Uses `@WebMvcTest` for lightweight controller slice testing
- Mocks service dependencies (no database or full Spring Boot context)
- Uses MockMvc for HTTP request/response testing
- Excludes security auto-configuration for isolated controller testing
- **Requires EndpointTestConfiguration** - provides @SpringBootConfiguration for @WebMvcTest

#### EndpointTestConfiguration

Since `user-module-endpoint` is a library module (not a standalone application), it lacks a `@SpringBootApplication` class. The `@WebMvcTest` annotation requires a `@SpringBootConfiguration` class to be found, so we provide one for tests:

**Location**: `user-module-endpoint/src/test/java/com/comex/usermodule/endpoint/EndpointTestConfiguration.java`

This configuration:
- **Provides @SpringBootConfiguration** - Required by @WebMvcTest
- **Enables auto-configuration** - Loads minimal Spring Boot configuration for web tests
- **Lives in test source** - Only used during testing, not packaged with the module
- **Discovered automatically** - @WebMvcTest scans the test package for @SpringBootConfiguration

#### UserControllerTest

**Setup:**
- Uses `@WebMvcTest(controllers = UserController.class)` for controller slice testing
- Excludes `SecurityAutoConfiguration` and `OAuth2ClientAutoConfiguration` to isolate controller logic
- Uses `@Import({UserController.class, UserWebMapper.class})` to import required beans
- Mocks service dependencies with `@MockBean` (UserService, UserAuthenticationService, UserVerificationService)
- Autowires MockMvc as `sut` (system under test)

**Test Methods (8 tests):**
1. `testCreateUser` - Create user endpoint
2. `testCreateUserWithVariousInputs` - Parameterized test for edge cases
3. `testLogin` - Login endpoint
4. `testVerify` - Email verification endpoint
5. `testFindByEmail` - Get user by email
6. `testFindByEmailWithMultipleRoles` - User with multiple roles

**Key Principles:**
- Controller tests are isolated from service and persistence layers
- Services are mocked - tests verify controller behavior only
- Uses MockMvc to simulate HTTP requests
- Verifies HTTP status codes and JSON response structure
- Depends on user-module-core test-jar for test data (UserTestInventory)

#### Running Endpoint Tests

**Important:** Before running endpoint tests, ensure `user-module-core` is installed to make the test-jar available:

```bash
# Install core module (builds test-jar)
./mvnw clean install -pl user-module-core -am

# Run endpoint tests
./mvnw test -pl user-module-endpoint
```

**Why this is needed:**
- Endpoint tests depend on `user-module-core:test-jar` for test data
- The test-jar artifact must be available in your local Maven repository
- Running `mvn install` on core module creates and installs the test-jar

**Last Updated**: October 2025
**Maintained By**: Comex Development Team
