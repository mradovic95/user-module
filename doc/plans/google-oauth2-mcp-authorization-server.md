# Plan: Google login for easy-eval REST API and MCP (Claude-connector style OAuth)


## Context

**Goal.** In `~/my_projects/easy-eval`, use `user-module` so that:

1. Users can sign in with Google and then call the REST API (frontend keeps using the user-module JWT).
2. The easy-eval MCP server (`/mcp`, Streamable HTTP, Spring AI 2.0) behaves like the official
   Atlassian connector: in Claude Code `/mcp` shows **Needs authentication → Authenticate**, a browser
   opens, the user confirms with Google, and tools work for that user. Same for claude.ai / Claude
   Desktop custom connectors.

**Is it possible?** Yes. The piece that is missing is an **OAuth 2.1 authorization server** that Claude
can talk to. Claude's MCP client never talks to Google directly (Google has no Dynamic Client
Registration and would not know our users). The standard pattern ("DCR proxy" / federated IdP) is:

```
Claude Code / claude.ai                easy-eval (one JVM)                            Google
───────────────────────                ───────────────────────────────────────────    ──────
POST /mcp (no token) ───────────────▶  401  WWW-Authenticate: Bearer resource_metadata="…/.well-known/oauth-protected-resource/mcp"
GET /.well-known/oauth-protected-resource/mcp ─▶ {resource, authorization_servers:[issuer]}
GET /.well-known/oauth-authorization-server ──▶ RFC 8414 metadata (authorize/token/register/jwks, S256)
POST /oauth2/register (DCR, RFC 7591) ───────▶ client_id for this Claude instance (public client)
open browser: GET /oauth2/authorize?code_challenge&resource=…/mcp
                                       not logged in → redirect /oauth2/authorization/google ──▶ Google login
                                       ◀── /login/oauth2/code/google (user provisioned in user-module DB)
                                       saved request → back to /oauth2/authorize → consent → code
◀── redirect http://localhost:<port>/callback?code=…   (claude.ai: https://claude.ai/api/mcp/auth_callback)
POST /oauth2/token (PKCE verifier, resource) ─▶ RS256 JWT  sub=email, roles=…, aud=…/mcp  (+ refresh token)
POST /mcp  Authorization: Bearer <jwt> ──────▶ resource-server chain validates sig/iss/aud → tools run as that user
```

The authorization server is built once, as a **reusable optional module of user-module**, and
easy-eval (and later other comex apps) consume it through configuration. Google login itself already
exists in user-module (uncommitted 0.0.8 work); it is reused for both flows.

### Facts that constrain the design (verified against current docs)

- Claude requires: a `401` with `WWW-Authenticate: Bearer resource_metadata="…"` to start sign-in;
  RFC 9728 protected-resource metadata whose `resource` equals the MCP URL **exactly as entered** in
  Claude; RFC 8414 metadata reachable at `/.well-known/…`; PKCE S256 and
  `code_challenge_methods_supported: ["S256"]`; DCR (`registration_endpoint`) unless CIMD is
  advertised; token endpoint accepting `application/x-www-form-urlencoded`; discovery/registration/token
  endpoints answering within 10 s; `invalid_grant` on dead refresh tokens; refresh-token rotation for
  public clients.
- Redirect URIs: Claude Code uses `http://localhost:<ephemeral port>/callback` (also `127.0.0.1`);
  hosted Claude uses `https://claude.ai/api/mcp/auth_callback`. DCR registers the exact URI per
  client, so exact matching works; we only allow-list the hosts.
- Claude Code refreshes on 401 and proactively 5 min before expiry; `claude mcp login/logout <name>`,
  `/mcp` → Authenticate / Re-authenticate.
- Spec says authorization-server endpoints MUST be HTTPS, redirect URIs localhost or HTTPS.
  `http://localhost:8081` works for local development; production needs a public **HTTPS** host that
  bypasses the REST API Gateway (which buffers and caps at 29 s, already documented as unsuitable
  for `/mcp`).
- Libraries: Spring Boot 4.1.x manages Spring Authorization Server 2.0; Boot 4 starter names are
  `spring-boot-starter-security-oauth2-authorization-server` and
  `spring-boot-starter-security-oauth2-resource-server` (confirmed on Maven Central).
  `org.springaicommunity:mcp-authorization-server-spring-boot:0.1.14` (Spring AI 2.0 line, **no**
  Spring AI dependency, only `spring-security-oauth2-authorization-server`) adds anonymous DCR at
  `/oauth2/register` and RFC 8707 resource → `aud`. Reference implementation of the exact pattern
  (SAS + DCR + upstream IdP login, Boot 4.1 / Java 25): github.com/lukas-grigis/spring-oauth2-mcp.

### Current state (what we build on)

- **user-module (0.0.8-SNAPSHOT, uncommitted):** Boot 4.1.1 / Java 25 upgrade + Google OAuth2
  hardening. `SecurityConfiguration.userModuleSecurityFilterChain` is one STATELESS chain that
  conditionally adds `oauth2Login` when a `ClientRegistrationRepository` exists;
  `OAuth2LoginSuccessHandler` emits the HS256 user JWT (redirect `?token=` or JSON);
  `UserGoogleSpringAuthenticator` enforces `user.oauth2.allowed-domains` and provisions via
  `UserService.createOAuth2User`. No refresh tokens, no authorization server, no resource server.
  **Latent problem:** the chain is `@ConditionalOnMissingBean(SecurityFilterChain.class)`, so an app
  with its own chain (easy-eval) silently loses Google login entirely.
- **easy-eval:** Boot 4.1.0, Spring AI 2.0.0, `user-module-starter-postgre:0.0.7` + three Boot-4
  shims, own `SecurityConfig` (single chain: `ApiTokenAuthFilter` (`eek_` tokens, consumes the
  header) → user-module `JwtAuthFilter`; `/mcp` not public). Identity is read as
  `authentication.getName()` = email in `SpringSecurityIdentityDirectory`. MCP tools in
  `infrastructure/inbound/mcp/*Tools.java`. Frontend: React, email/password only, JWT in
  localStorage, `ProtectedRoute`. Infra: single EC2 + API Gateway (raw execute-api URL, no custom
  domain/TLS), port 8081 open for MCP bypass.

### Decisions / assumptions

- Two token kinds coexist: the existing HS256 user JWT for REST/frontend, and RS256 authorization
  server JWTs (aud-bound) for MCP clients. `eek_` API tokens keep working on `/mcp` (CI / SUT
  scripts). HS256 user JWTs are no longer accepted on `/mcp` (nothing uses that).
- DCR is the client-registration mechanism (Claude falls back to it; CIMD not supported by SAS).
- Consent page stays on for DCR clients (spec requirement for a federated proxy; SAS ships a default
  page). The consent page shows the client's redirect host.
- Google login moves into its own ordered `SecurityFilterChain` in user-module so it works with
  app-defined chains and serves both flows (REST token hand-off and authorization-server login).
- Persistence of registered clients / authorizations / consents: JDBC (Liquibase) for the Postgres
  starter; in-memory fallback otherwise. Signing key configurable (PEM) so tokens survive restarts.
- Out of scope: DynamoDB persistence for the authorization server (in-memory there), OIDC
  `id_token`, enterprise managed auth, directory listing.

---

## Phase 0 — Prerequisites (no code)

- Google Cloud console, existing OAuth client: add redirect URIs
  `http://localhost:8081/login/oauth2/code/google` and `https://<easy-eval-host>/login/oauth2/code/google`.
- Pick the public HTTPS host for easy-eval (see Phase 5); it becomes `user.oauth2.authorization-server.issuer`.
- This plan lives at `doc/plans/google-oauth2-mcp-authorization-server.md` (done).

## Phase 1 — user-module: land the pending 0.0.8 work and fix the Google-login chain

Repo: `/Users/mihailoradovic/my_projects/user-module` (uncommitted Boot 4 + Google OAuth2 work).

1. Build and run the full suite as-is (`./mvnw clean install`, JAVA_HOME per `.sdkmanrc`), then commit
   the current working tree as the Boot 4 + Google OAuth2 change (tests already moved to
   `user-module-configuration/src/test`). Remove stray `pom.xml.backup`.
2. Extract Google login into its own chain, in
   `user-module-configuration/src/main/java/com/comex/usermodule/configuration/OAuth2GoogleConfiguration.java`
   (imported **after** `SecurityConfiguration` by both starters, so the main chain's
   `@ConditionalOnMissingBean(SecurityFilterChain.class)` is evaluated before this bean exists):
   - `@Bean @Order(HIGHEST_PRECEDENCE + 20) @ConditionalOnBean(ClientRegistrationRepository.class)
     SecurityFilterChain userModuleGoogleLoginSecurityFilterChain(HttpSecurity, OAuth2LoginSuccessHandler)`
     with `securityMatcher("/oauth2/authorization/**", "/login/oauth2/**")`, session
     `IF_REQUIRED`, csrf off, `oauth2Login(o -> o.successHandler(...))`.
   - Delete the runtime `if (clientRegistrationRepository…) http.oauth2Login(...)` block from
     `SecurityConfiguration.userModuleSecurityFilterChain`; keep `/oauth2/**`, `/login/oauth2/**`
     public there only for backwards compatibility of the matcher list.
3. Make `OAuth2LoginSuccessHandler` saved-request aware: extend
   `SavedRequestAwareAuthenticationSuccessHandler`; after the existing email / `email_verified` /
   domain checks and user provisioning (`userGoogleAuthenticator.authenticate(...)`), if
   `requestCache.getRequest(req, res) != null` (user arrived from `/oauth2/authorize`) call
   `super.onAuthenticationSuccess` instead of emitting the JWT. Existing behaviour is unchanged when
   there is no saved request.
4. Tests (conventions: `sut`, GIVEN/WHEN/THEN, no `@Nested`/`@DisplayName`):
   - `SecurityConfigurationTest`: Google login chain is present when client props set **and** when the
     app defines its own `SecurityFilterChain`; `/oauth2/authorization/google` still redirects to Google.
   - `OAuth2LoginSuccessHandlerTest`: new case "saved request present → redirects to it, no token".
5. Update `README.md` / `CLAUDE.md` wording ("an application-defined SecurityFilterChain replaces the
   module's **API** chain; Google login chain is kept").

## Phase 2 — user-module: new module `user-module-authorization-server`

New Maven module (add to parent `pom.xml` `<modules>`, dependency management, and as an
**optional** dependency of both starters so it is on the classpath but inert until enabled).
Package `com.comex.usermodule.authorizationserver`. Dependencies: `user-module-configuration`,
`spring-boot-starter-security-oauth2-authorization-server`,
`org.springaicommunity:mcp-authorization-server-spring-boot:0.1.14` (version property in parent),
`spring-security-oauth2-jose` (for `JwtDecoder`, transitively present), test: core test-jar,
`spring-boot-starter-security-test`, `spring-boot-starter-webmvc-test`.

### 2.1 Properties — `AuthorizationServerProperties`, prefix `user.oauth2.authorization-server`

| property | default | purpose |
|---|---|---|
| `enabled` | `false` | gates the whole auto-config (`@ConditionalOnProperty`) |
| `issuer` | required | public base URL, e.g. `https://api.easy-eval.com` (`http://localhost:8081` in dev) |
| `resources` | `[]` | protected resource URIs relative to issuer, e.g. `/mcp`; each gets a metadata document and is an accepted `aud` |
| `scopes-supported` | `[]` | advertised scopes (Claude requests these); may stay empty |
| `allowed-redirect-hosts` | `localhost,127.0.0.1,claude.ai` | DCR redirect-URI host allow-list |
| `access-token-ttl` / `refresh-token-ttl` | `PT1H` / `P30D` | token lifetimes |
| `consent-required` | `true` | consent page for DCR clients |
| `jwk.private-key-pem`, `jwk.kid` | blank | RSA signing key; blank ⇒ generated at boot + WARN |

### 2.2 `AuthorizationServerConfiguration` (`@AutoConfiguration`, conditional on `enabled`)

- `@Bean @Order(HIGHEST_PRECEDENCE + 10) SecurityFilterChain userModuleAuthorizationServerSecurityFilterChain(HttpSecurity)`:
  `securityMatcher` = the authorization-server endpoints matcher (from
  `OAuth2AuthorizationServerConfigurer`/`McpAuthorizationServerConfigurer`: `/oauth2/authorize`,
  `/oauth2/token`, `/oauth2/register`, `/oauth2/jwks`, `/oauth2/revoke`, `/oauth2/introspect`,
  `/.well-known/oauth-authorization-server`, `/.well-known/openid-configuration`); session
  `IF_REQUIRED`; `.with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), c -> ...)`
  with DCR enabled; `exceptionHandling` entry point for HTML requests =
  `LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google")` (so an unauthenticated
  `/oauth2/authorize` goes straight to Google; saved request is kept in the session by the default
  `HttpSessionRequestCache`, which the Google chain from Phase 1 then honours).
- `AuthorizationServerSettings` bean: issuer from properties.
- `JWKSource<SecurityContext>` bean: RSA key from `jwk.*` or generated; `JwtDecoder` for the
  resource side built in-process via `OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)`
  (no HTTP self-fetch at startup).
- `OAuth2TokenCustomizer<JwtEncodingContext>` (`UserModuleAccessTokenCustomizer`): for access tokens
  set `sub` = Google `email` attribute of the `OAuth2AuthenticationToken` principal, `email`,
  `roles` = comma-joined `user.getAuthorities()` from `UserService.findByEmail(email)` (same shape as
  `JwtService.generateToken`), and make sure `aud` carries the `resource` from the request
  (mcp-security does this; verify, else set it here).
- Client/authorization persistence:
  - `@ConditionalOnBean(DataSource.class)` + `user.persistence.type=postgresql`:
    `JdbcRegisteredClientRepository`, `JdbcOAuth2AuthorizationService`,
    `JdbcOAuth2AuthorizationConsentService`; Liquibase changelog
    `user-module-infrastructure-postgre/src/main/resources/db/changelog/2_create-oauth2-authorization-server-tables.yml`
    (SAS schema: `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`),
    added to `user-master.yml`. Verify mcp-security's DCR honours an application-provided
    `RegisteredClientRepository`; if not, register a thin wrapper.
  - otherwise in-memory implementations.
- DCR hardening: validate every `redirect_uris` entry against `allowed-redirect-hosts`
  (loopback hosts: any port) and force `require-proof-key=true`, `require-authorization-consent=consent-required`,
  grant types `authorization_code,refresh_token`, auth method `none`. Where mcp-security exposes a
  registration customizer use it; otherwise decorate the `RegisteredClientRepository.save`.
- Refresh tokens for public clients: Spring Authorization Server historically does **not** issue a
  refresh token when the client authenticates with `none`. Check SAS 2.0 behaviour first; if still
  the case, add a customised `OAuth2AuthorizationCodeAuthenticationProvider`/token generator path
  (or follow what mcp-security does) so Claude gets a rotated refresh token. Fallback: longer
  `access-token-ttl` (12h) and users hit **Re-authenticate** when it expires.
- Login/consent UX: default SAS consent page is acceptable for v1; display the client's redirect
  host (`localhost:<port>` vs `claude.ai`) on it (spec recommendation for loopback clients).

### 2.3 Protected-resource helpers (`...authorizationserver.resource`)

Reusable by any app that protects an endpoint with these tokens:

- `ProtectedResourceMetadataController` (or `RouterFunction`): `GET /.well-known/oauth-protected-resource`
  and `/.well-known/oauth-protected-resource/<resource-path>` for each configured resource →
  `{"resource": "<issuer><path>", "authorization_servers": ["<issuer>"], "bearer_methods_supported": ["header"], "scopes_supported": [...]}`.
  Registered as public in the authorization-server chain's matcher or by the consuming app.
- `BearerResourceMetadataEntryPoint implements AuthenticationEntryPoint`: writes
  `401` + `WWW-Authenticate: Bearer resource_metadata="<issuer>/.well-known/oauth-protected-resource/<path>"`
  (+ `scope="..."` when scopes configured) and the module's JSON error body.
- `@Bean JwtDecoder userModuleAuthorizationServerJwtDecoder`: in-process decoder with validators
  issuer == `issuer` and `aud` ∩ `resources` ≠ ∅.
- `@Bean Converter<Jwt, AbstractAuthenticationToken> userModuleJwtAuthenticationConverter`:
  principal name = `sub` (email), authorities from the `roles` claim split on `,` (no `SCOPE_`
  prefix), so consumers that read `authentication.getName()` keep working.

### 2.4 Tests (`user-module-authorization-server/src/test/...`)

`WebApplicationContextRunner` + MockMvc with `springSecurity()`, like `SecurityConfigurationTest`:

- `AuthorizationServerConfigurationTest`: inert when disabled; metadata documents correct
  (`issuer`, `registration_endpoint`, `code_challenge_methods_supported=[S256]`,
  `token_endpoint_auth_methods_supported` contains `none`); DCR accepts `localhost:<port>/callback`
  and `https://claude.ai/api/mcp/auth_callback`, rejects other hosts; unauthenticated
  `/oauth2/authorize` redirects to `/oauth2/authorization/google`; full authorization-code + PKCE
  exchange with a mocked `OAuth2AuthenticationToken` yields an RS256 JWT with `sub`=email, `roles`,
  `aud`; `JwtDecoder` rejects wrong `aud`.
- `ProtectedResourceMetadataControllerTest`, `BearerResourceMetadataEntryPointTest`,
  `UserModuleAccessTokenCustomizerTest` (unit, Mockito on `UserService`).
- Postgres integration test for the Liquibase tables + `JdbcRegisteredClientRepository` round-trip
  (extends `AbstractPostgresIntegrationTest`).

### 2.5 Docs and release

- `README.md`: new section "MCP / OAuth 2.1 authorization server" (properties, Google console
  redirect URIs, Claude Code / claude.ai registration commands, HTTPS requirement).
  `CLAUDE.md`: module table, directory tree, testing section. Update `doc/ubiquitous-language.md`
  if terms are introduced (authorization server, protected resource, DCR).
- Release `0.0.8` to CodeArtifact via `build-and-push.yml` once Phases 1–2 are green
  (`./mvnw clean install` locally first so easy-eval can consume the SNAPSHOT meanwhile).

## Phase 3 — easy-eval backend

Repo: `/Users/mihailoradovic/my_projects/easy-eval`.

1. `pom.xml`: `user-module.version` → `0.0.8`; add `user-module-authorization-server` (if not pulled
   by the starter) and `spring-boot-starter-security-oauth2-resource-server`. Drop the three Boot-4
   shims that 0.0.8 makes redundant (`authenticationProvider` and `jwtAuthFilterRegistration` in
   `SecurityConfig`, the `hibernate-types-60` exclusion, the `@ComponentScan/@EntityScan` on
   `EvalApplication`) — verify each one individually by booting the app before deleting it.
2. `application.yml`:
   ```yaml
   spring.security.oauth2.client.registration.google:
     client-id: ${GOOGLE_CLIENT_ID}
     client-secret: ${GOOGLE_CLIENT_SECRET}
     scope: openid,profile,email
   user:
     oauth2:
       success-redirect-url: ${EASY_EVAL_FRONTEND_URL:http://localhost:5173}/auth/callback
       allowed-domains: ${EASY_EVAL_GOOGLE_ALLOWED_DOMAINS:}        # empty = anyone
       authorization-server:
         enabled: true
         issuer: ${EASY_EVAL_PUBLIC_URL:http://localhost:8081}
         resources: [ "${EVAL_MCP_ENDPOINT:/mcp}" ]
         allowed-redirect-hosts: [ localhost, 127.0.0.1, claude.ai ]
         jwk:
           private-key-pem: ${EASY_EVAL_OAUTH2_SIGNING_KEY:}
   ```
   Add the new env vars to the GitHub-secrets table in `easy-eval-infrastructure/README.md` and to
   the `docker run -e …` line in `.github/workflows/build-and-push.yml`.
3. `config/SecurityConfig.java` → three chains (user-module adds the authorization-server and
   Google-login chains by itself):
   - **MCP chain** `@Order(1)`, `securityMatcher("/mcp/**")`: cors, csrf off, STATELESS,
     `addFilterBefore(apiTokenAuthFilter, LogoutFilter.class)` (unchanged, `eek_` keeps working),
     `.oauth2ResourceServer(o -> o.jwt(j -> j.decoder(userModuleAuthorizationServerJwtDecoder)
     .jwtAuthenticationConverter(userModuleJwtAuthenticationConverter)))`,
     `authenticationEntryPoint(bearerResourceMetadataEntryPoint)`, `anyRequest().authenticated()`.
     `JwtAuthFilter` is **not** on this chain, so RS256 tokens are never rejected by it.
   - **API chain** (existing, lowest order): add `permitAll` for `/.well-known/oauth-protected-resource/**`;
     everything else unchanged. Update the class Javadoc ("REST and MCP are one chain" is no longer true).
4. `SpringSecurityIdentityDirectory` needs no change (`JwtAuthenticationToken.getName()` = `sub` = email,
   authorities from `roles`). Add an ArchUnit exception only if the new resource-server imports trip
   the existing "only these packages name `org.springframework.security`" rule.
5. `spring.ai.mcp.server.instructions`: one sentence telling agents that the server supports OAuth
   sign-in (Authenticate in `/mcp`) in addition to API tokens. Update `docs/`, `README.md`
   registration snippet (`claude mcp add --transport http easy-eval <url>/mcp`, then `/mcp` →
   Authenticate), and `CLAUDE.md` "Access" section.
6. Tests (Testcontainers Postgres, existing patterns):
   - `/mcp` without credentials → `401` with `WWW-Authenticate: Bearer resource_metadata="…/mcp"`;
     `/.well-known/oauth-protected-resource/mcp` public and `resource` = `<issuer>/mcp`;
     `/.well-known/oauth-authorization-server` public.
   - `/mcp` with a valid `eek_` token still lists tools; with an authorization-server JWT (minted in
     test via the `JwtEncoder`/`JWKSource` beans) the tool runs as that email; wrong `aud` → 401.
   - REST endpoints reject authorization-server JWTs? (not required; document either way).

## Phase 4 — easy-eval-frontend (Google sign-in for the REST API)

Repo: `/Users/mihailoradovic/my_projects/easy-eval-frontend`.

- `src/pages/LoginPage.jsx` (and `RegisterPage.jsx`): "Continue with Google" button →
  `window.location.assign(`${VITE_EVAL_API_BASE_URL}/oauth2/authorization/google`)`.
- New public route `/auth/callback` (`src/pages/AuthCallbackPage.jsx`): read `token` or `error` from
  the query string; on token call the existing `AuthContext` login-with-token path (same localStorage
  key `token`), `navigate('/')`; on error show the mapped message (`email_missing`,
  `email_not_verified`, `domain_not_allowed`) and link back to `/login`. Add to the router and to the
  public-route list in `ProtectedRoute`.
- Amplify env: `VITE_EVAL_API_BASE_URL` must be the same public URL as the backend issuer.

## Phase 5 — infrastructure and Claude registration

Repo: `/Users/mihailoradovic/my_projects/easy-eval-infrastructure`.

- **HTTPS host bypassing API Gateway** (required by Claude for non-localhost): simplest is a DNS name
  (Route53) for the EIP plus a TLS-terminating reverse proxy on the EC2 box (Caddy or nginx +
  Let's Encrypt, in the same docker host) forwarding to `:8081`. Alternative: ALB + ACM certificate.
  Only this host serves `/mcp`, `/oauth2/**`, `/login/oauth2/**`, `/.well-known/**`; keep the REST
  gateway as-is or point the frontend to the new host too. Open 443 in the security group.
- Secrets: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `EASY_EVAL_PUBLIC_URL`,
  `EASY_EVAL_FRONTEND_URL`, `EASY_EVAL_OAUTH2_SIGNING_KEY` (base64 PEM) as GitHub secrets → `docker run -e`.
- Claude registration (document in easy-eval README):
  ```bash
  claude mcp add --transport http easy-eval https://<host>/mcp     # or http://localhost:8081/mcp locally
  claude mcp login easy-eval        # or /mcp → Authenticate inside Claude Code
  ```
  claude.ai / Desktop: Settings → Connectors → Add custom connector → URL `https://<host>/mcp`
  (leave OAuth client fields empty: DCR). API-token users keep `--header "Authorization: Bearer eek_…"`.

## Verification

1. **user-module:** `./mvnw clean install` green (core test-jar first). `SecurityConfigurationTest`
   and the new authorization-server tests pass.
2. **easy-eval local:** start Postgres (`docker-compose`), run app with Google client env vars.
   ```bash
   curl -i http://localhost:8081/mcp -X POST            # expect 401 + WWW-Authenticate … resource_metadata
   curl -s http://localhost:8081/.well-known/oauth-protected-resource/mcp | jq
   curl -s http://localhost:8081/.well-known/oauth-authorization-server | jq   # S256, registration_endpoint
   ```
3. **MCP Inspector:** `npx @modelcontextprotocol/inspector`, connect to `http://localhost:8081/mcp`
   with OAuth → browser → Google → tools list (its callback `http://localhost:6274/oauth/callback` is
   covered by the loopback allow-list).
4. **Claude Code:** `claude mcp add --transport http easy-eval http://localhost:8081/mcp`, open
   `claude`, `/mcp` shows *Needs authentication* → Authenticate → Google → *Connected*; call
   `list_datasets`; confirm the row belongs to the Google account's user in the DB. Restart the app:
   the stored token still works (JDBC + persisted key). `claude mcp logout easy-eval` then re-auth.
5. **API token path:** `claude mcp add … --header "Authorization: Bearer eek_…"` still works; CI
   SUT scripts untouched.
6. **Frontend:** "Continue with Google" → `/auth/callback?token=` → logged in, REST calls succeed;
   `domain_not_allowed` path when `allowed-domains` excludes the account.
7. **Hosted:** after Phase 5, add `https://<host>/mcp` as a custom connector in claude.ai; the
   consent page shows `claude.ai`; tools work; token refresh observed in logs after `access-token-ttl`.

## Risks / open points

- Refresh tokens for public (DCR) clients in Spring Authorization Server — may need a custom
  provider; otherwise re-authentication every access-token TTL.
- mcp-security 0.1.14 is pre-1.0 (Spring AI 2.0 line); verify it compiles against Boot 4.1.1 /
  Spring Security 7.1 and that its DCR path accepts a custom `RegisteredClientRepository` and
  redirect-host validation. Fallback: implement the `/oauth2/register` endpoint ourselves on plain SAS.
- Open DCR endpoint: rate-limit it at the reverse proxy and prune stale clients (job or SQL) — note
  Claude registers a new client on every fresh connection.
- Bean-ordering subtlety in user-module: the Google chain and the authorization-server chain must be
  registered after `SecurityConfiguration`'s main chain so its `@ConditionalOnMissingBean` keeps
  working; covered by `SecurityConfigurationTest`.
- Sessions on the EC2 single instance are in-memory; fine for one container, revisit if scaled out.

---

## Status (2026-10-05)

Implemented and committed:

- **Phase 1** — user-module `bca16c9`: Google login in its own chain, saved-request-aware success handler.
- **Phase 2** — user-module `c9aac7b`: `user-module-authorization-server` module, Liquibase tables, docs; README
  is now tracked (the `.gitignore` entry `Readme.md` had hidden it). Full build green, 43 new tests.
- **Phase 3** — easy-eval `dea81a9` + `b0e9eb4`: user-module `0.0.8-SNAPSHOT`, `/mcp` chain, Google + authorization
  server configuration, `McpSecurityIntegrationTest`, deploy workflow secrets and optional Caddy front door.
- **Phase 4** — easy-eval-frontend `49ed6a5`: "Continue with Google", `/auth/callback`.
- **Phase 5** — easy-eval-infrastructure `64a6748`: README (secrets, HTTPS front door). No Terraform change was
  needed: the EC2 security group already opens 80/443.

Deviations from the plan worth knowing:

- Spring Security 7.1 withholds refresh tokens from public clients and only authenticates them on the PKCE code
  exchange; the module adds `PublicClientRefreshTokenGenerator` and a client-id-only converter/provider for the
  refresh grant.
- Spring Security 7.1's resource-server DSL auto-registers its own protected-resource-metadata filter; the
  module's filter is anchored right after `CorsFilter` so it answers first.
- easy-eval now requires `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` to start (Boot rejects an empty client
  registration).

Left for the operator:

1. Release user-module `0.0.8` to CodeArtifact (`build-and-push.yml`); easy-eval's CI cannot resolve the SNAPSHOT.
2. Google Cloud console: add `http://localhost:8081/login/oauth2/code/google` and
   `https://<EASY_EVAL_DOMAIN>/login/oauth2/code/google` to the OAuth client's redirect URIs.
3. Choose a DNS name for the backend, point its A record at the EIP, set the new GitHub secrets
   (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `EASY_EVAL_DOMAIN`, `EASY_EVAL_PUBLIC_URL`, `EASY_EVAL_FRONTEND_URL`,
   `EASY_EVAL_OAUTH2_SIGNING_KEY`, optionally `EASY_EVAL_GOOGLE_ALLOWED_DOMAINS`) and deploy.
4. Run the manual verification above (MCP Inspector, Claude Code `/mcp` → Authenticate, claude.ai custom connector).
