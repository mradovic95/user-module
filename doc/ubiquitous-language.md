# Ubiquitous Language

Canonical vocabulary for `user-module`. Maintained by `/ubiquitous-language` — every term cites where it lives.

## Identity & Account

| Term            | Definition                                                                                                                                     | Aliases to avoid                                                 | Where it lives                                                                         |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **User**        | A registered account holder in the system, identified by email, with a username, password, roles, status, creation time, and verification code | UserEntity, UserDynamoEntity, UserResponse, user_table, users    | `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:16`         |
| **User status** | The lifecycle state of a user account, which is either Created or Verified                                                                     | UserStatus, status                                               | `user-module-core/src/main/java/com/comex/usermodule/core/domain/UserStatus.java:3`    |
| **Create user** | The registration of a new user account, given a default user role and a status determined by the verification policy                           | createUser, register, sign up                                    | `user-module-core/src/main/java/com/comex/usermodule/core/service/UserService.java:25` |
| **Email**       | The user's unique address serving as their primary identity, used for email login, as the JWT subject, and as the main lookup key in persistence | username (the password grant's parameter for it), subject       | `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:22`         |
| **Username**    | The user's display name, a field distinct from the email address                                                                               | username (as a name for the JWT subject/email)                   | `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:19`         |

## Authentication & Tokens

| Term             | Definition                                                                                                                   | Aliases to avoid                                       | Where it lives                                                                                       |
|------------------|------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| **Email login**  | The authentication of a user by their email address and password that yields a signed access token                           | login, sign in, authenticate                           | `user-module-core/src/main/java/com/comex/usermodule/core/service/UserAuthenticationService.java:15` |
| **Google login** | Sign-in through a user's Google account via OAuth2, after which the module issues its own access token                       | UserGoogleAuthenticator, OAuth2 login, Google sign-in  | `user-module-core/src/main/java/com/comex/usermodule/core/port/UserGoogleAuthenticator.java:5`       |
| **Access token** | The RS256 JWT the authorization server issues on every login (email, Google, password grant, MCP flow), carrying the user's email as subject, their authorities as the `roles` claim and every protected resource as audience | JWT, JWT token, token, login token, LoginTokenResponse | `user-module-core/src/main/java/com/comex/usermodule/core/port/AccessTokenIssuer.java:9`, `user-module-endpoint/src/main/java/com/comex/usermodule/endpoint/model/LoginTokenResponse.java:3`   |

## Verification

| Term                      | Definition                                                                                                                                                         | Aliases to avoid                                                   | Where it lives                                                                                      |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| **Created**               | The status of a newly registered user who has not yet completed verification, assigned when verification is required                                               | CREATED, pending                                                   | `user-module-core/src/main/java/com/comex/usermodule/core/domain/UserStatus.java:4`                 |
| **Verified**              | The status of a user whose account has been confirmed, reached either through successful verification or immediately at creation when verification is not required | VERIFIED, active                                                   | `user-module-core/src/main/java/com/comex/usermodule/core/domain/UserStatus.java:5`                 |
| **Verification code**     | A random UUID assigned to a user at creation that serves as the proof presented to verify the account                                                              | verificationCode, verification_code, code, verification-code-index | `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:25`                      |
| **Verification required** | A configuration policy stating whether new users start as Created and must verify their account, or are Verified immediately at creation                           | verificationRequired, verification-required                        | `user-module-configuration/src/main/java/com/comex/usermodule/configuration/UserProperties.java:14` |
| **Verify**                | The confirmation of a user's account by presenting their verification code, which marks the user as Verified                                                       | activate, confirm                                                  | `user-module-core/src/main/java/com/comex/usermodule/core/service/UserVerificationService.java:22`  |

## Authorization

| Term           | Definition                                                                                                                   | Aliases to avoid                        | Where it lives                                                                                                                                                                        |
|----------------|------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Role**       | A named grouping of permissions that is assigned to a user to grant access rights                                            | RoleEntity, RoleDynamoEntity            | `user-module-core/src/main/java/com/comex/usermodule/core/domain/Role.java:15`                                                                                                        |
| **Permission** | A named capability held by a role that authorizes specific actions                                                           | PermissionEntity, permission (DB table) | `user-module-core/src/main/java/com/comex/usermodule/core/domain/Role.java:18`                                                                                                        |
| **Authority**  | An access-control grant belonging to a user, consisting of the union of their role names and those roles' permission strings | getAuthorities, roles (JWT claim name)  | `user-module-core/src/main/java/com/comex/usermodule/core/domain/Role.java:24`                                                                                                        |
| **User role**  | The default role assigned to every newly created user, seeded as ROLE_USER                                                   | ROLE_USER                               | `user-module-infrastructure-postgre/src/main/resources/db/changelog/insert-initial-roles.sql:2`, `user-module-core/src/main/java/com/comex/usermodule/core/mapper/UserMapper.java:30` |
| **Admin role** | The seeded role designating administrator users, named ROLE_ADMIN                                                            | ROLE_ADMIN                              | `user-module-infrastructure-postgre/src/main/resources/db/changelog/insert-initial-roles.sql:5`                                                                                       |

## Events

| Term                    | Definition                                                                                   | Aliases to avoid  | Where it lives                                                                             |
|-------------------------|----------------------------------------------------------------------------------------------|-------------------|--------------------------------------------------------------------------------------------|
| **User created event**  | A domain event published after a new user is created, carrying the user's username and email | UserCreatedEvent  | `user-module-core/src/main/java/com/comex/usermodule/core/event/UserCreatedEvent.java:12`  |
| **User verified event** | A domain event published after a user is verified, carrying the verification code            | UserVerifiedEvent | `user-module-core/src/main/java/com/comex/usermodule/core/event/UserVerifiedEvent.java:12` |

## Ambiguities

### "username" — 2 senses

| Sense | Meaning                                                                                                                     | Evidence                                                                                                                                                                                 |
|-------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | The user's display name, a real field on **User** distinct from email                                                       | `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:19`                                                                                                           |
| 2     | The OAuth2 `password` grant's form parameter (and Spring's `UserDetails.getUsername()`), which is actually the **Email**; the token subject is set from `user.getEmail()` | `user-module-authorization-server/src/main/java/com/comex/usermodule/authorizationserver/PasswordGrantAuthenticationConverter.java:28`, `user-module-authorization-server/src/main/java/com/comex/usermodule/authorizationserver/JwtAccessTokenIssuer.java:54` |

**Resolution:** use **Username** only for the display-name field and **Email** for the login identity and JWT subject.
The password grant's `username` parameter name is fixed by RFC 6749, so it stays on the wire; call the value email in code.

### "user table" — 2 spellings

| Sense | Meaning                                                      | Evidence                                                                                            |
|-------|--------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| 1     | PostgreSQL table persisting **User**, named `user_table`     | `user-module-infrastructure-postgre/src/main/resources/db/changelog/0_create-user-table.yml:13`     |
| 2     | DynamoDB table persisting **User**, named `users` by default | `user-module-configuration/src/main/java/com/comex/usermodule/configuration/UserProperties.java:28` |

**Resolution:** in conversation and docs, say "the user table" and name the concrete store only when the persistence
technology matters.

## Relationships

- A **User** holds one or more **Roles** —
  `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:21`
- A **Role** holds zero or more **Permissions** —
  `user-module-core/src/main/java/com/comex/usermodule/core/domain/Role.java:18`
- A **User**'s **Authorities** are the union of their role names and those roles' permissions —
  `user-module-core/src/main/java/com/comex/usermodule/core/domain/User.java:27`
- Every new **User** receives the **User role** at creation —
  `user-module-core/src/main/java/com/comex/usermodule/core/mapper/UserMapper.java:30`
