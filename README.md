# User Module

A **multi-module Maven** reusable Spring Boot authentication and user management library designed to be integrated into larger applications.
Provides JWT-based authentication (RS256 tokens from its own OAuth 2.1 authorization server), OAuth2 (Google) login, email verification, and role-based access control with support
for both PostgreSQL and DynamoDB persistence strategies.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Integration Guide](#integration-guide)
    - [Step 1: Add Maven Dependency](#step-1-add-maven-dependency)
    - [Step 2: Persistence Strategy Comparison](#step-2-persistence-strategy-comparison)
    - [Step 3: Configure Application Properties](#step-3-configure-application-properties)
    - [Step 4: Database Setup](#step-4-database-setup)
    - [Step 5: Configure Authentication Strategy](#step-5-configure-authentication-strategy)
    - [Step 6: Include Database Migrations (PostgreSQL only)](#step-6-include-database-migrations-postgresql-only)
    - [Step 7: Understanding Auto-Configuration](#step-7-understanding-auto-configuration)
- [Authentication Strategies](#authentication-strategies)
    - [Username/Password Authentication](#usernamepassword-authentication)
    - [Google OAuth2 Authentication](#google-oauth2-authentication)
    - [How Both Strategies Work Together](#how-both-strategies-work-together)
    - [MCP / OAuth 2.1 Authorization Server](#mcp--oauth-21-authorization-server)
- [API Endpoints](#api-endpoints)
- [Configuration Properties Reference](#configuration-properties-reference)
- [Customization](#customization)
- [CI/CD Pipeline](#cicd-pipeline)
    - [Workflows](#workflows)
    - [AWS CodeArtifact Repository](#aws-codeartifact-repository)
    - [Release Process](#release-process)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

- **JWT Authentication** - RS256 access tokens issued by the module's own OAuth 2.1 authorization server, for
  password login, Google login and MCP clients alike
- **OAuth2 Google Login** - Social login integration
- **Email Verification** - Verification-code based user email verification
- **Role-Based Access Control** - Flexible permission system
- **Dual Persistence Support** - Choose between PostgreSQL or DynamoDB via starter modules
- **Auto-Configuration** - Spring Boot auto-configuration for easy integration
- **Hexagonal Architecture** - Clean separation of concerns across modules
- **Multi-Module Maven** - Modular architecture with 8 independent modules
- **Production Ready** - Includes error handling and security best practices

---

## Technology Stack

- **Java 25**
- **Spring Boot 4.1.1**
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - PostgreSQL persistence
- **AWS DynamoDB SDK** - DynamoDB persistence
- **Spring Authorization Server** - Token issuing (RS256 JWTs); validation via Spring's OAuth2 resource server
- **Liquibase** - Database migrations (PostgreSQL only)
- **SpringDoc OpenAPI 3.1.1** - API documentation
- **Testcontainers 2.0.5** - Integration testing

---

## Architecture

The project is organized as a **multi-module Maven project** with 8 modules following **Hexagonal Architecture** (Ports & Adapters):

```
user-module (parent)
├── user-module-core                      # Domain models, services, ports
├── user-module-infrastructure-postgre    # PostgreSQL persistence adapter
├── user-module-infrastructure-dynamodb   # DynamoDB persistence adapter
├── user-module-endpoint                  # REST controllers and web models
├── user-module-configuration             # Auto-configuration and security
├── user-module-authorization-server      # OAuth 2.1 authorization server, issues every token
├── user-module-starter-postgre           # PostgreSQL starter (all-in-one)
└── user-module-starter-dynamodb          # DynamoDB starter (all-in-one)
```

**Key Benefits:**

- **Modular Design**: Each module has a single, well-defined responsibility
- **Flexible Integration**: Choose PostgreSQL or DynamoDB via starter dependency
- **Technology Independence**: Core business logic has no Spring dependencies
- **Independent Testing**: Each module can be tested in isolation
- **Easy to Extend**: Add new persistence implementations without changing core logic

---

## Quick Start

Get started in 3 steps:

1. **Add the dependency** to your `pom.xml`
2. **Configure** your `application.yml` with database settings and the token issuer URL
3. **Run** your application - the module auto-configures itself

The user-module uses Spring Boot auto-configuration to seamlessly integrate with your application. No manual bean configuration required!

---

## Integration Guide

### Step 1: Add Maven Dependency

Choose the appropriate **starter module** based on your persistence strategy:

#### Option A: PostgreSQL Starter

```xml
<dependency>
    <groupId>com.comex</groupId>
    <artifactId>user-module-starter-postgre</artifactId>
    <version>0.0.10-SNAPSHOT</version>
</dependency>
```

#### Option B: DynamoDB Starter

```xml
<dependency>
    <groupId>com.comex</groupId>
    <artifactId>user-module-starter-dynamodb</artifactId>
    <version>0.0.10-SNAPSHOT</version>
</dependency>
```

**What's included in each starter:**
- ✅ Core module (domain, services, ports), transitively
- ✅ Configuration module (security, auto-configuration)
- ✅ Authorization server module (issues every token, default API chain)
- ✅ Infrastructure module (PostgreSQL OR DynamoDB)

**Not included:**
- The endpoint module (`/user` REST controller). Add `com.comex:user-module-endpoint` (same version) and
  component-scan `com.comex.usermodule.endpoint`; no auto-configuration registers it.
- PostgreSQL: `spring-boot-starter-liquibase`. Spring Boot 4 only runs Liquibase with it on the classpath; the
  starter brings `liquibase-core` alone.

The PostgreSQL starter declares `@EnableJpaRepositories`/`@EntityScan` for the module's own packages, which turns
Spring Boot's JPA auto-scan off for your application: list your own repository and entity packages explicitly.

If using AWS CodeArtifact, configure the repository (see [Consuming the Artifact](#consuming-the-artifact)):

```xml
<repositories>
    <repository>
        <id>user-module</id>
        <url>https://com-comex-925199373191.d.codeartifact.us-east-2.amazonaws.com/maven/user-module/</url>
    </repository>
</repositories>
```

### Step 2: Persistence Strategy Comparison

| Option | Best For | Setup Complexity | Starter Artifact |
|--------|----------|------------------|------------------|
| **PostgreSQL** | Traditional relational data, existing PostgreSQL infrastructure | Low (auto-migrations) | `user-module-starter-postgre` |
| **DynamoDB** | AWS-native applications, serverless architectures | Medium (manual table creation) | `user-module-starter-dynamodb` |

### Step 3: Configure Application Properties

Add configuration to your **`application.yml`** in your project:

#### Option A: PostgreSQL Configuration (when using user-module-starter-postgre)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your-database
    username: your-username
    password: your-password
  jpa:
    hibernate:
      ddl-auto: none  # Let Liquibase manage schema
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/user-master.yml  # User-module migrations

user:
  verification-required: true  # Enable email verification
  oauth2:
    authorization-server:
      issuer: https://api.example.com          # Required. Public base URL of this app; token issuer
      resources: [ /api ]                      # Protected resource paths; every token carries all of them as aud
      first-party-client-id: my-frontend       # Optional. Public client allowed the password grant
      access-token-ttl: 1h
      jwk:
        private-key-pem: ${OAUTH2_SIGNING_KEY}  # PKCS#8 RSA key; omit to generate one per start
```

#### Option B: DynamoDB Configuration (when using user-module-starter-dynamodb)

```yaml
user:
  persistence:
    type: dynamodb   # required: the DynamoDB repository is only registered for this value
  dynamodb:
    table-name: users
    region: us-east-1
    # endpoint: http://localhost:8000  # Uncomment for local DynamoDB testing
  verification-required: true
  oauth2:
    authorization-server:
      issuer: https://api.example.com
      resources: [ /api ]
      jwk:
        private-key-pem: ${OAUTH2_SIGNING_KEY}
```

**Note**: `user.persistence.type` defaults to `postgresql`, so the PostgreSQL starter needs nothing; with the
DynamoDB starter set it to `dynamodb`, otherwise no `UserRepository` is registered and the application does not
start.

**Important:** `user.oauth2.authorization-server.issuer` is required; the application does not start without it
("user.oauth2.authorization-server.issuer must be set"). Every token is signed RS256 with the `jwk` key. Without
`private-key-pem` a key is generated at startup, so every issued token dies with the process. Generate a key for
production and store the PEM in a secret:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048
```

### Step 4: Database Setup

#### For PostgreSQL:

**No manual setup required** (beyond `spring-boot-starter-liquibase`, see Step 1). The module includes Liquibase
migrations that create all necessary tables and initial data when your application starts.

**What gets created:**
- `user_table` - User accounts
- `role` - User roles
- `permission` - Role permissions
- `users_roles` - User-to-role mappings
- `roles_permissions` - Role-to-permission mappings
- `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent` - Authorization server storage
- Default roles: `ROLE_USER`, `ROLE_ADMIN`
- Seed users: `test-user-1..3@somemail.com` (`ROLE_USER`) and `admin@somemail.com` (`ROLE_ADMIN`), all `VERIFIED`

#### For DynamoDB:

Create the DynamoDB table **before** starting your application:

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

**Table Structure:**
- **Primary Key:** `email` (String)
- **Global Secondary Index:** `verification-code-index` on `verificationCode`
- **Data Model:** Denormalized (roles and permissions embedded in user documents)

### Step 5: Configure Authentication Strategy

The module ships a default, stateless JWT `SecurityFilterChain` (an OAuth2 resource server accepting the tokens of
its own authorization server). Username/password login is always on; Google OAuth2 login switches itself on as soon
as Google client credentials are present in your configuration.

#### What's Enabled By Default:

**Username/Password Authentication:**
- ✅ Always available
- ✅ `POST /user/login` returns an RS256 access token issued by the authorization server
- ✅ With `first-party-client-id` set, the same login is available as the OAuth2 `password` grant on
  `/oauth2/token` (with rotated refresh tokens)
- ✅ The module's API chain validates tokens with `oauth2ResourceServer().jwt()` (signature, issuer, audience)
- ✅ Public endpoints: `POST /user`, `POST /user/login`, `GET /user/verify`, `/oauth2/**`, `/login/oauth2/**`,
  `/swagger-ui/**`, `/v3/api-docs/**`
- ✅ Every other request requires a valid `Authorization: Bearer <jwt>` header and gets a JSON `401`/`403` otherwise

**Google OAuth2 Authentication:**
- ✅ Beans always registered (`OAuth2GoogleConfiguration`)
- ✅ `oauth2Login()` runs in the module's own Google login chain, registered only when a
  `spring.security.oauth2.client.registration.*` entry (e.g. `google.*`) is set
- ✅ Users are created automatically from the Google profile (email as username, status `VERIFIED` regardless of
  `user.verification-required`, since Google has already verified the email)
- ✅ The same RS256 access token is issued after a successful Google login

#### To Enable Google OAuth2 Login:

Add your Google credentials to `application.yml`. Spring Security's built-in Google provider supplies the
authorization, token and user-info endpoints and the `openid, profile, email` scopes.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id.apps.googleusercontent.com
            client-secret: your-google-client-secret

user:
  oauth2:
    # Optional. Where to send the browser after a successful Google login.
    success-redirect-url: https://your-frontend.example.com/oauth/callback
    # Optional. Only Google accounts from these email domains may sign in. Empty = anyone.
    allowed-domains:
      - comex.com
      - leanpay.com
```

Then start the flow by sending the browser to `GET /oauth2/authorization/google`.

**How the token is handed back:**
- With `user.oauth2.success-redirect-url` set, the browser is redirected to
  `<success-redirect-url>?token=<jwt>`.
- Without it, the response body is `{"token": "<jwt>"}` with `Content-Type: application/json`.

**Restricting who can sign in:**

`user.oauth2.allowed-domains` limits Google login to accounts whose email domain (the part after `@`) is in the
list. The match is case-insensitive and exact. When the list is empty or not set, every Google account is
accepted. The check runs for new and existing users alike, so removing a domain from the list also blocks users
created earlier. The password-based `POST /user` registration is not affected.

| Google account | `allowed-domains: [comex.com, leanpay.com]` |
|---|---|
| `ana@comex.com` | Allowed |
| `Mihailo@LEANPAY.com` | Allowed (case-insensitive) |
| `someone@gmail.com` | Rejected, no user created |
| `attacker@comex.com.evil.net` | Rejected (exact domain match, not a prefix) |

Independently of the list, a Google account whose `email_verified` claim is `false` is rejected.

**How rejections are delivered:** the same way as the token. With `success-redirect-url` set, the browser is
redirected to `<success-redirect-url>?error=<code>`; without it, the response is a JSON `{"error": "..."}` with the
HTTP status. Codes: `email_missing` (401), `email_not_verified` (401), `domain_not_allowed` (403).

**How to get Google credentials:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Create OAuth 2.0 credentials (Web application)
4. Add authorized redirect URI: `http://your-domain.com/login/oauth2/code/google`
   - For local development: `http://localhost:8080/login/oauth2/code/google`
   - For production: `https://your-domain.com/login/oauth2/code/google`

#### Authentication Modes Summary:

| Mode | Configuration Required | Available Endpoints |
|------|------------------------|---------------------|
| **Username/Password only** | None (default) | `POST /user`, `POST /user/login`, `GET /user/verify` |
| **Username/Password + Google** | Add Google credentials | Above plus `GET /oauth2/authorization/google` |

#### Customizing Security

Define your own `SecurityFilterChain` bean and the module's API chain (`userModuleSecurityFilterChain`) backs
off; the module's own chains (Google login, authorization server) are kept. The token decoder
(`userModuleAuthorizationServerJwtDecoder`), the authentication converter (`userModuleJwtAuthenticationConverter`),
the `AuthenticationProvider` and the `OAuth2LoginSuccessHandler` beans remain available so you can reuse them in
your own chain (see "Extend Security Configuration" under Customization).

### Step 6: Include Database Migrations (PostgreSQL only)

**This step is only required if using PostgreSQL.**

The user-module includes Liquibase migrations. To integrate them into your project:

#### Method 1: Reference the Master Changelog (Recommended)

In your main Liquibase changelog file (e.g., `src/main/resources/db/changelog/master.yml`), include the user-module migrations:

```yaml
databaseChangeLog:
  - include:
      file: classpath:db/changelog/user-master.yml  # User-module migrations
  - include:
      file: db/changelog/your-app-migrations.yml    # Your application migrations
```

#### Method 2: Configure in application.yml

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/user-master.yml
```

**Note:** If you have existing migrations, you may need to create a master changelog that includes both user-module and your application migrations.

**What's in user-master.yml:**
- `0_create-user-table.yml` - Creates users table
- `1_create-role-and-permission-tables.yml` - Creates roles and permissions
- `2_create-oauth2-authorization-server-tables.yml` - Creates the authorization server tables
- `insert-initial-roles.yml` - Inserts the default `ROLE_USER` and `ROLE_ADMIN` roles
- `insert-initial-users.yml` - Inserts three test users and an `admin` user (always applied; it is part of
  `user-master.yml`)

### Step 7: Understanding Auto-Configuration

The user-module uses **Spring Boot Auto-Configuration** to automatically set up all required beans. Here's what happens:

#### What Gets Auto-Configured:

1. **Security Configuration**
   - OAuth 2.1 authorization server: token endpoint, DCR, metadata, RS256 signing key (the one token issuer)
   - Default API chain validating those tokens as an OAuth2 resource server
   - OAuth2 login success handler (if OAuth2 is configured)
   - Public endpoints: `POST /user`, `POST /user/login`, `GET /user/verify`, `/oauth2/**`, `/login/oauth2/**`, Swagger
   - Protected endpoints: Everything else requires valid JWT token

2. **Persistence Layer**
   - UserRepository implementation (PostgreSQL or DynamoDB based on configuration)
   - Database entity mappers
   - JPA configuration (if PostgreSQL) or DynamoDB client (if DynamoDB)

3. **Core Services**
   - UserService - User CRUD operations
   - UserAuthenticationService - Authentication delegation
   - UserVerificationService - Email verification
   - AccessTokenIssuer (port) - Token issuing, implemented by the authorization server (`JwtAccessTokenIssuer`)

4. **Event Publishing**
   - EventPublisher - Publishes domain events (user created, verified, etc.)

#### Auto-Configuration Class:

The main auto-configuration is defined per starter:
```
com.comex.usermodule.starter.postgre.UserModuleAutoConfiguration
com.comex.usermodule.starter.dynamodb.UserModuleAutoConfiguration
```
plus `com.comex.usermodule.authorizationserver.UserModuleAuthorizationServerAutoConfiguration` from the
authorization server module.

These classes are automatically loaded via Spring Boot's auto-configuration mechanism defined in:
```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

#### How Tokens Are Validated:

The default API chain (`userModuleSecurityFilterChain`, registered by `UserModuleAuthorizationServerAutoConfiguration`)
is a Spring Security OAuth2 resource server. For every request it:

1. Extracts the token from the `Authorization: Bearer <token>` header
2. Validates signature (RS256, the authorization server's key, in-process), issuer, expiry and audience (`aud` must
   contain a configured resource)
3. Authenticates the request with the principal name = `sub` (the user's email) and authorities from the `roles` claim
4. Allows/denies the request based on endpoint security rules (JSON `401`/`403`)

**You don't need to manually configure the filter chain** - it's auto-configured.

#### Customizing Auto-Configuration:

If you need to customize a bean, provide your own `@Bean` of the same type in your application's `@Configuration` class. Spring Boot's `@ConditionalOnMissingBean` will skip the auto-configured bean.

Example - Override the access token lifetime:

```yaml
# application.yml
user:
  oauth2:
    authorization-server:
      access-token-ttl: 2h  # instead of the default 1 hour
```

Example - Custom event publisher (create your own @Configuration class):

```
@Bean
public EventPublisher eventPublisher() {
    return new KafkaEventPublisher();  // Your custom implementation
}
```

---

## Authentication Strategies

The user-module supports **both authentication strategies simultaneously by default**. Users can choose their preferred login method.

### Username/Password Authentication

Traditional email/password authentication with JWT tokens. **Always enabled.**

**Flow:**
1. User registers: `POST /user` with email, password, username
2. (Optional) User verifies email: `GET /user/verify?code=xyz`
3. User logs in: `POST /user/login` with email and password
4. Server returns an RS256 access token (issued by the authorization server)
5. Client includes token in subsequent requests: `Authorization: Bearer <token>`

**Endpoints:**
- `POST /user` - Create account
- `POST /user/login` - Login
- `GET /user/verify?code={code}` - Verify email
- `POST /oauth2/token` with `grant_type=password` - Login as an OAuth2 client (access + refresh token)

**Configuration:** None required (enabled by default). The password grant needs
`user.oauth2.authorization-server.first-party-client-id`:

```bash
curl -X POST https://api.example.com/oauth2/token \
  -d grant_type=password -d username=user@example.com -d password='SecurePass123!' -d client_id=my-frontend
# refresh later: -d grant_type=refresh_token -d refresh_token=<token> -d client_id=my-frontend
```

The first-party client is a public client (no secret) registered at startup with only the `password` and
`refresh_token` grants; refresh tokens are rotated. A wrong password is `400 invalid_grant`; a client without the
`password` grant (every dynamically registered / MCP client) is `400 unauthorized_client`.

### Google OAuth2 Authentication

Social login with Google accounts. **Enabled by adding Google credentials.**

**Flow:**
1. User clicks "Login with Google"
2. Client redirects to: `GET /oauth2/authorization/google`
3. User authenticates with Google
4. Google redirects back to your application
5. Server creates the user on first login (existing users are reused) and returns an RS256 access token
6. Client includes token in subsequent requests: `Authorization: Bearer <token>`

**Endpoints:**
- `GET /oauth2/authorization/google` - Initiate Google login

**Configuration:** Add `client-id` and `client-secret` to `application.yml` (see Step 5)

**Important:**
- Google OAuth2 users are automatically created in your database on first login
- The OAuth2 beans are **always loaded**; the login chain itself is registered only when the Google
  `client-id`/`client-secret` properties are present
- Google login runs in its **own session-based security chain** (`/oauth2/authorization/**`, `/login/oauth2/**`),
  so it keeps working even if your application replaces the module's API chain with its own `SecurityFilterChain`
- If the user was sent to Google by another chain (for example an OAuth2 authorization server's
  `/oauth2/authorize`), the success handler resumes that saved request instead of issuing a token

### How Both Strategies Work Together

**Email Matching:** The module intelligently merges user accounts based on email address.

**Scenarios:**

1. **User registers with email/password first:**
   - User creates account: `user@example.com` with password
   - Later, user logs in with Google using `user@example.com`
   - ✅ Existing account is used (matched by email)
   - ✅ User can now login with either method

2. **User logs in with Google first:**
   - User clicks "Login with Google" and uses `user@example.com`
   - ✅ New account created automatically from Google profile, with a random password (the module has no
     password-reset flow, so such a user signs in with Google only)

3. **User wants to use both methods:**
   - ✅ Users can login with email/password OR Google OAuth2
   - ✅ Both methods access the same user account (matched by email)

**Benefits:**
- Flexibility: Users choose their preferred authentication method
- Security: Option to use Google's authentication
- Convenience: Single account, multiple login options
- No configuration needed: Works out of the box (OAuth2 just needs credentials)

### MCP / OAuth 2.1 Authorization Server

MCP clients (Claude Code, claude.ai / Claude Desktop custom connectors, the MCP Inspector) cannot call
`POST /user/login`. They expect the standard MCP authorization flow: a `401` pointing at protected resource
metadata, authorization server metadata, Dynamic Client Registration, PKCE and a token endpoint. The
`user-module-authorization-server` module (pulled in by both starters; it is also the issuer of the login tokens
above) provides exactly that and reuses the Google login for the human part of the flow, so users sign in with
Google in the browser and the client gets its own tokens.

```
MCP client ──POST /mcp──▶ 401 WWW-Authenticate: Bearer resource_metadata="…/.well-known/oauth-protected-resource/mcp"
           ──GET /.well-known/oauth-protected-resource/mcp ──▶ { resource, authorization_servers: [issuer] }
           ──GET /.well-known/oauth-authorization-server ────▶ endpoints, S256, registration_endpoint
           ──POST /oauth2/register ───────────────────────────▶ client_id (public client, PKCE)
           ──browser: /oauth2/authorize ──▶ Google login ──▶ back to /oauth2/authorize ──▶ code to the client
           ──POST /oauth2/token (code + PKCE verifier) ──────▶ RS256 access token (sub = email, roles, aud = all resources)
                                                                + rotated refresh token
```

**1. The module** comes with both starters; there is nothing to add or switch on.

**2. Configure** (Google client properties from Step 5 are required for the browser part of the flow):

```yaml
user:
  oauth2:
    authorization-server:
      issuer: https://api.example.com        # required; public base URL of this app; http://localhost:8081 in dev
      resources: [ /mcp, /api ]              # protected resource paths; every token carries ALL of them as aud
      allowed-redirect-hosts: [ localhost, 127.0.0.1, claude.ai ]
      access-token-ttl: 1h
      refresh-token-ttl: 30d
      jwk:
        private-key-pem: ${OAUTH2_SIGNING_KEY}   # PKCS#8 RSA key; omit to generate one per start
```

Generate a signing key with `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048` and store the PEM in a
secret. With PostgreSQL, registered clients, authorizations and consents are stored in the
`oauth2_registered_client`, `oauth2_authorization` and `oauth2_authorization_consent` tables (created by the
module's Liquibase changelog); otherwise they live in memory and are lost on restart.

Add `https://<issuer host>/login/oauth2/code/google` (and the `http://localhost:8081/...` variant for development)
to the redirect URIs of your Google OAuth client. Authorization server endpoints must be served over HTTPS for
anything but `localhost`.

**3. Protect your resource** with the beans the module provides, in a chain of your own:

```
@Bean
@Order(1)
SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http,
        @Qualifier("userModuleAuthorizationServerJwtDecoder") JwtDecoder jwtDecoder,
        @Qualifier("userModuleJwtAuthenticationConverter") JwtAuthenticationConverter jwtAuthenticationConverter,
        @Qualifier("userModuleBearerResourceMetadataEntryPoint") AuthenticationEntryPoint entryPoint) throws Exception {
    return http
        .securityMatcher("/mcp/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(rs -> rs
            .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
            .authenticationEntryPoint(entryPoint))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
        .build();
}
```

Defining this chain, like any application `SecurityFilterChain`, switches the module's default API chain off, so
define your API chain as well (see "Extend Security Configuration"). The decoder validates signature, issuer and
audience in-process; the converter sets the principal name to the
user's email and maps the `roles` claim to authorities. The module's default API chain uses the same two beans, and
since every token's `aud` lists every configured resource, one token (from `/user/login`, Google login, the password
grant or the MCP flow) works on every resource of the application, whatever `resource` parameter the client sent.

**4. Connect from Claude:**

```bash
claude mcp add --transport http my-app https://api.example.com/mcp
claude mcp login my-app          # or /mcp → Authenticate inside Claude Code
```

In claude.ai or Claude Desktop add the same URL as a custom connector and leave the OAuth client fields empty
(Dynamic Client Registration).

**Notes:**
- Scopes are optional. When `scopes-supported` is empty no consent screen is shown; configure at least one scope
  to get Spring's consent page for dynamically registered clients.
- Refresh tokens are issued to public clients and rotated on every use, as the MCP specification requires.
- Behind a reverse proxy set `server.forward-headers-strategy=framework` so redirects use the public host.
- The module registers an extra chain (`userModuleAuthorizationServerSecurityFilterChain`) for the OAuth2
  endpoints and the metadata documents; your own `SecurityFilterChain` beans keep working.

---

## API Endpoints

Once the module is integrated, these endpoints become available in your application:

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| `POST` | `/user` | Create new user account | `{ "email": "...", "password": "...", "username": "..." }` |
| `POST` | `/user/login` | Login with email/password | `{ "email": "...", "password": "..." }` |
| `GET` | `/user/verify?code={code}` | Verify email address | - |
| `GET` | `/oauth2/authorization/google` | Initiate Google OAuth2 login | - |

### Protected Endpoints (Require JWT Token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/user?email={email}` | Find a user by email |

All other endpoints in your application are automatically protected and require a valid access token (RS256, issued
by the module's authorization server) in the `Authorization` header:

```
Authorization: Bearer eyJraWQiOiI...
```

### Example Usage

**Create User:**

```bash
curl -X POST http://your-app.com/user \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "username": "johndoe"
  }'
```

**Login:**

```bash
curl -X POST http://your-app.com/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**

```json
{
  "token": "eyJraWQiOiI..."
}
```

**Use Token in Protected Requests:**

```bash
curl -X GET http://your-app.com/api/protected-resource \
  -H "Authorization: Bearer eyJraWQiOiI..."
```

**Verify Email:**

```bash
curl http://your-app.com/user/verify?code=abc123xyz
```

---

## Configuration Properties Reference

All user-module properties use the `user` prefix in your `application.yml`:

### Required Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `user.oauth2.authorization-server.issuer` | String | - | **Required**. Public base URL of the app; issuer of every token |

### Optional Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `user.verification-required` | Boolean | `false` | Enable/disable email verification requirement |
| `user.persistence.type` | String | `postgresql` | `postgresql` or `dynamodb`; must be `dynamodb` with the DynamoDB starter |

### PostgreSQL Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.datasource.url` | String | - | JDBC connection URL |
| `spring.datasource.username` | String | - | Database username |
| `spring.datasource.password` | String | - | Database password |
| `spring.liquibase.enabled` | Boolean | `true` | Enable Liquibase migrations |
| `spring.liquibase.change-log` | String | - | Path to Liquibase changelog |

### DynamoDB Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `user.dynamodb.table-name` | String | `users` | DynamoDB table name |
| `user.dynamodb.region` | String | `us-east-1` | AWS region |
| `user.dynamodb.endpoint` | String | - | Custom endpoint (for local testing only) |

### OAuth2 Properties (Google)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.security.oauth2.client.registration.google.client-id` | String | - | Google OAuth2 client ID |
| `spring.security.oauth2.client.registration.google.client-secret` | String | - | Google OAuth2 client secret |
| `spring.security.oauth2.client.registration.google.scope` | List | - | OAuth2 scopes (e.g., `email`, `profile`) |
| `user.oauth2.success-redirect-url` | String | - | Browser is redirected here with `?token=` after Google login; blank = JSON body |
| `user.oauth2.allowed-domains` | List | - | Email domains allowed to sign in with Google; empty = everyone |

### Authorization Server Properties (`user-module-authorization-server`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `user.oauth2.authorization-server.issuer` | String | - | Public base URL of the app; token issuer (required) |
| `user.oauth2.authorization-server.resources` | List | `[]` | Protected resource paths, e.g. `/mcp`; every token carries all of them as `aud` |
| `user.oauth2.authorization-server.scopes-supported` | List | `[]` | Scopes advertised to clients; empty = no consent screen |
| `user.oauth2.authorization-server.allowed-redirect-hosts` | List | `localhost, 127.0.0.1, claude.ai` | Hosts allowed in registered redirect URIs (loopback: any port, http) |
| `user.oauth2.authorization-server.access-token-ttl` | Duration | `1h` | Access token lifetime |
| `user.oauth2.authorization-server.refresh-token-ttl` | Duration | `30d` | Refresh token lifetime (always rotated) |
| `user.oauth2.authorization-server.consent-required` | Boolean | `true` | Consent for registered clients (shown only when scopes are requested) |
| `user.oauth2.authorization-server.login-path` | String | `/oauth2/authorization/google` | Where unauthenticated users are sent to log in |
| `user.oauth2.authorization-server.first-party-client-id` | String | - | Public client registered at startup with the `password` + `refresh_token` grants; blank = none |
| `user.oauth2.authorization-server.jwk.private-key-pem` | String | - | PKCS#8 RSA signing key; blank = generated per start |
| `user.oauth2.authorization-server.jwk.kid` | String | - | Key id; derived from the key when blank |

---

## Customization

### Override Default Beans

The module uses `@ConditionalOnMissingBean` annotations on most beans, allowing you to override them by providing your own implementation.

**Example: Custom Event Publisher**

Create a configuration class in your application:

```
@Configuration
public class CustomConfiguration {

    @Bean
    public EventPublisher eventPublisher() {
        // Your custom implementation (e.g., Kafka, RabbitMQ)
        return new KafkaEventPublisher();
    }
}
```

### Extend Security Configuration

Defining **any** `SecurityFilterChain` bean in your application replaces the module's default API chain
(`userModuleSecurityFilterChain`). The module's Google login and authorization server chains are kept. In that
case validate the module's tokens in your own chain with `oauth2ResourceServer().jwt()` and the module's decoder and
converter beans (`UserModuleAuthorizationServerAutoConfiguration.JWT_DECODER_BEAN_NAME` /
`JWT_AUTHENTICATION_CONVERTER_BEAN_NAME`):

```
@Configuration
@EnableWebSecurity
public class ApplicationSecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
            @Qualifier(UserModuleAuthorizationServerAutoConfiguration.JWT_DECODER_BEAN_NAME) JwtDecoder jwtDecoder,
            @Qualifier(UserModuleAuthorizationServerAutoConfiguration.JWT_AUTHENTICATION_CONVERTER_BEAN_NAME)
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/user", "/user/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/user/verify").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .build();
    }
}
```

### Access Current User

In your controllers, you can access the authenticated user:

```
@RestController
public class MyController {

    @GetMapping("/profile")
    public String getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getSubject();  // The user's email; roles are the request's authorities
    }
}
```

### Listen to User Events

The default `EventPublisher` (`EventPublisherMock`) only logs events. To receive them as Spring application
events, provide an `EventPublisher` bean that forwards to `ApplicationEventPublisher`, then listen:

```
@Component
public class UserEventListener {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        // Send welcome email, create user profile, etc.
        log.info("User created: {}", event.getEmail());
    }

    @EventListener
    public void handleUserVerified(UserVerifiedEvent event) {
        // Grant access, send confirmation, etc.
        log.info("User verified: {}", event.getId());  // carries id, timestamp and verificationCode
    }
}
```

---

## CI/CD Pipeline

The user-module uses GitHub Actions for continuous integration and deployment to AWS CodeArtifact.

### Workflows

#### 1. Build Workflow (`.github/workflows/build.yml`)

**Triggers:**
- Push to `master` branch
- Pull requests to `master` branch
- Manual workflow dispatch

**Actions:**
- Checks out source code
- Sets up JDK 25 (Temurin distribution)
- Runs `mvn clean verify` to build and test the module
- Uses Maven cache for faster builds

**Purpose:** Validates that all code changes build successfully and pass all tests before merging.

#### 2. Build and Push Workflow (`.github/workflows/build-and-push.yml`)

**Triggers:**
- Manual workflow dispatch only (triggered by maintainers)

**Actions:**

1. **Checkout and Setup**
   - Checks out source code
   - Sets up JDK 25 with Maven cache

2. **AWS Authentication**
   - Configures AWS credentials using GitHub secrets
   - Retrieves CodeArtifact authorization token
   - Region: `us-east-2`

3. **Version Management**
   - Reads current version from `pom.xml` (e.g., `0.0.6-SNAPSHOT`)
   - Calculates release version by removing `-SNAPSHOT` suffix (e.g., `0.0.6`)
   - Increments patch version for next snapshot (e.g., `0.0.7-SNAPSHOT`)
   - Updates `pom.xml` with next snapshot version
   - Commits version change to repository

4. **Artifact Deployment**
   - Temporarily sets version to release version (e.g., `0.0.6`)
   - Deploys artifact to AWS CodeArtifact: `user-module` repository
   - Reverts `pom.xml` to snapshot version

**Version Strategy:**
- **Development:** Uses `-SNAPSHOT` versions (e.g., `0.0.6-SNAPSHOT`)
- **Release:** Removes `-SNAPSHOT` for deployment (e.g., `0.0.6`)
- **Auto-increment:** Automatically bumps to next snapshot after release

### AWS CodeArtifact Repository

**Repository Details:**
- **Domain:** `com-comex`
- **Repository:** `user-module`
- **Region:** `us-east-2`
- **URL:** `https://com-comex-925199373191.d.codeartifact.us-east-2.amazonaws.com/maven/user-module/`

**Artifact Coordinates:**
```xml
<groupId>com.comex</groupId>
<artifactId>user-module</artifactId>
<version>0.0.10-SNAPSHOT</version>
```

### Consuming the Artifact

To use the published artifact in your project, configure AWS CodeArtifact in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>user-module</id>
        <url>https://com-comex-925199373191.d.codeartifact.us-east-2.amazonaws.com/maven/user-module/</url>
    </repository>
</repositories>
```

And authenticate with AWS CodeArtifact (using `settings.xml` or AWS CLI):

```bash
# Export CodeArtifact auth token
export CODEARTIFACT_AUTH_TOKEN=$(aws codeartifact get-authorization-token \
  --domain com-comex \
  --domain-owner 925199373191 \
  --region us-east-2 \
  --query authorizationToken \
  --output text)

# Configure Maven settings
aws codeartifact login --tool maven \
  --domain com-comex \
  --domain-owner 925199373191 \
  --repository user-module \
  --region us-east-2
```

### Release Process

**For Maintainers:**

1. **Ensure all changes are merged to master**
   - All PRs should be reviewed and merged
   - Build workflow should pass on master

2. **Trigger deployment**
   - Go to GitHub Actions
   - Select "Build And Push To Code Artifact" workflow
   - Click "Run workflow" on `master` branch

3. **Automated steps**
   - Workflow builds and tests the module
   - Increments version (e.g., `0.0.6-SNAPSHOT` → `0.0.7-SNAPSHOT`)
   - Deploys release version `0.0.6` to CodeArtifact
   - Commits new snapshot version to repository

4. **Verify deployment**
   - Check AWS CodeArtifact console for new version
   - Verify version was incremented in `pom.xml` on master

**Version History Example:**
```
Initial: 0.0.5-SNAPSHOT
Deploy → Releases 0.0.5 to CodeArtifact, bumps to 0.0.6-SNAPSHOT
Deploy → Releases 0.0.6 to CodeArtifact, bumps to 0.0.7-SNAPSHOT
```

### GitHub Secrets Required

The deployment workflow requires these secrets to be configured in GitHub:

| Secret Name | Description |
|-------------|-------------|
| `AWS_CODE_ARTIFACT_USER_ACCESS_KEY_ID` | AWS access key with CodeArtifact write permissions |
| `AWS_CODE_ARTIFACT_USER_SECRET_ACCESS_KEY` | AWS secret access key |

**Permissions Required:**
- `codeartifact:GetAuthorizationToken`
- `codeartifact:PublishPackageVersion`
- `codeartifact:PutPackageMetadata`

---

## Building from Source

If you need to build the multi-module project from source:

### Prerequisites

- Java 25 or higher
- Maven 3.9.9+ (the bundled wrapper uses 3.9.16)
- Docker (for running integration tests)

### Build Commands

```bash
# Clone repository
git clone <repository-url>
cd user-module

# Build and install all modules to local Maven repository
./mvnw clean install

# Build without running tests
./mvnw clean install -DskipTests

# Build specific module with dependencies
./mvnw clean install -pl user-module-starter-postgre -am

# Run tests only
./mvnw test
```

### Running Tests

```bash
# Run all tests in all modules
./mvnw test

# Run tests in a specific module
./mvnw test -pl user-module-core
./mvnw test -pl user-module-infrastructure-postgre
./mvnw test -pl user-module-endpoint

# Run specific test class in a module
./mvnw test -pl user-module-core -Dtest=UserServiceTest

# Install core module before running endpoint tests
./mvnw clean install -pl user-module-core -am
./mvnw test -pl user-module-endpoint
```

**Note:**
- Integration tests use Testcontainers and require Docker to be running
- Endpoint tests require `user-module-core` to be installed first (creates test-jar artifact)

---

## License

Copyright © 2025 Comex Development Team

---

## Support

For issues, questions, or contributions, please contact the Comex Development Team or refer to the internal
documentation at `CLAUDE.md`.
