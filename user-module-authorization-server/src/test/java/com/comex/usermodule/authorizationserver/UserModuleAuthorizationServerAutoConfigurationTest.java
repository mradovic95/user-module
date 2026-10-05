package com.comex.usermodule.authorizationserver;

import static com.comex.usermodule.authorizationserver.UserModuleAuthorizationServerAutoConfiguration.AUTHORIZATION_SERVER_CHAIN_BEAN_NAME;
import static com.comex.usermodule.authorizationserver.UserModuleAuthorizationServerAutoConfiguration.BEARER_ENTRY_POINT_BEAN_NAME;
import static com.comex.usermodule.authorizationserver.UserModuleAuthorizationServerAutoConfiguration.JWT_AUTHENTICATION_CONVERTER_BEAN_NAME;
import static com.comex.usermodule.authorizationserver.UserModuleAuthorizationServerAutoConfiguration.JWT_DECODER_BEAN_NAME;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_OAUTH2_NAME;
import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerJwtAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

import com.comex.usermodule.authorizationserver.resource.BearerResourceMetadataEntryPoint;
import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.configuration.OAuth2GoogleConfiguration;
import com.comex.usermodule.configuration.SecurityConfiguration;
import com.comex.usermodule.configuration.UserConfiguration;
import com.comex.usermodule.configuration.UserProperties;
import com.comex.usermodule.core.port.UserRepository;
import com.jayway.jsonpath.JsonPath;

class UserModuleAuthorizationServerAutoConfigurationTest {

	private static final String ISSUER = "http://localhost";
	private static final String MCP_RESOURCE = ISSUER + "/mcp";
	private static final String CLAUDE_CODE_CALLBACK = "http://localhost:3118/callback";
	private static final String CLAUDE_AI_CALLBACK = "https://claude.ai/api/mcp/auth_callback";
	private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
	private static final String STATE = "state-123";

	private final UserRepository userRepository = mock(UserRepository.class);

	private final WebApplicationContextRunner sut = runner(true);

	@BeforeEach
	void setUp() {
		when(userRepository.findByEmailOptional(DEFAULT_EMAIL)).thenReturn(Optional.of(verifiedUser()));
	}

	@Test
	void testDisabledByDefault() {
		// GIVEN
		WebApplicationContextRunner runner = runner(false);

		// WHEN
		runner.run(context -> {
			// THEN
			assertThat(context).doesNotHaveBean(AUTHORIZATION_SERVER_CHAIN_BEAN_NAME);
			assertThat(context).doesNotHaveBean(ProtectedResources.class);
			assertThat(context).doesNotHaveBean(JWT_DECODER_BEAN_NAME);
			assertThat(context).hasBean(SecurityConfiguration.API_CHAIN_BEAN_NAME);
		});
	}

	@Test
	void testRegistersChainsAndResourceSupportBeans() {
		// GIVEN
		// enabled runner

		// WHEN
		sut.run(context -> {
			// THEN
			assertThat(context).getBeans(SecurityFilterChain.class).containsOnlyKeys(
				AUTHORIZATION_SERVER_CHAIN_BEAN_NAME,
				OAuth2GoogleConfiguration.GOOGLE_LOGIN_CHAIN_BEAN_NAME,
				SecurityConfiguration.API_CHAIN_BEAN_NAME);
			// exactly one decoder, so an application's oauth2ResourceServer().jwt() picks it up unambiguously
			assertThat(context).hasSingleBean(JwtDecoder.class);
			assertThat(context).hasBean(JWT_DECODER_BEAN_NAME);
			assertThat(context).hasBean(JWT_AUTHENTICATION_CONVERTER_BEAN_NAME);
			assertThat(context).getBean(BEARER_ENTRY_POINT_BEAN_NAME).isInstanceOf(BearerResourceMetadataEntryPoint.class);
			ProtectedResources resources = context.getBean(ProtectedResources.class);
			assertThat(resources.issuer()).isEqualTo(ISSUER);
			assertThat(resources.resourceUris()).containsExactly(MCP_RESOURCE);
		});
	}

	@Test
	void testAuthorizationServerMetadata() {
		// GIVEN
		// enabled runner

		// WHEN
		sut.run(context -> {
			// THEN
			mockMvc(context).perform(get("/.well-known/oauth-authorization-server"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuer").value(ISSUER))
				.andExpect(jsonPath("$.authorization_endpoint").value(ISSUER + "/oauth2/authorize"))
				.andExpect(jsonPath("$.token_endpoint").value(ISSUER + "/oauth2/token"))
				.andExpect(jsonPath("$.registration_endpoint").value(ISSUER + "/oauth2/register"))
				.andExpect(jsonPath("$.code_challenge_methods_supported").value(org.hamcrest.Matchers.hasItem("S256")))
				.andExpect(jsonPath("$.token_endpoint_auth_methods_supported").value(org.hamcrest.Matchers.hasItem("none")))
				.andExpect(jsonPath("$.grant_types_supported").value(org.hamcrest.Matchers.hasItems("authorization_code", "refresh_token")));
		});
	}

	@Test
	void testProtectedResourceMetadata() {
		// GIVEN
		// enabled runner with resource /mcp

		// WHEN
		sut.run(context -> {
			MockMvc mockMvc = mockMvc(context);

			// THEN
			mockMvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(jsonPath("$.resource").value(MCP_RESOURCE))
				.andExpect(jsonPath("$.authorization_servers[0]").value(ISSUER))
				.andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"))
				.andExpect(jsonPath("$.scopes_supported").doesNotExist());
			// the root document describes the default (first) resource
			mockMvc.perform(get("/.well-known/oauth-protected-resource"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resource").value(MCP_RESOURCE));
			mockMvc.perform(get("/.well-known/oauth-protected-resource/unknown"))
				.andExpect(status().isNotFound());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { CLAUDE_CODE_CALLBACK, "http://127.0.0.1:55555/callback", CLAUDE_AI_CALLBACK })
	void testDynamicClientRegistrationAcceptsAllowedRedirectHosts(String redirectUri) {
		// GIVEN
		// default allowed hosts: localhost, 127.0.0.1, claude.ai

		// WHEN
		sut.run(context -> {
			// THEN
			String clientId = registerClient(mockMvc(context), redirectUri);
			assertThat(clientId).isNotBlank();
		});
	}

	@Test
	void testDynamicClientRegistrationToleratesRequestedScope() {
		// GIVEN
		// Spring's default validator rejects a scope in the registration; some MCP clients send one

		// WHEN
		sut.run(context -> {
			// THEN
			mockMvc(context).perform(post("/oauth2/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(registrationBodyWithScope("tools", CLAUDE_CODE_CALLBACK)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.client_id").isNotEmpty());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://evil.example.com/callback", "http://claude.ai/api/mcp/auth_callback",
		"https://claude.ai.evil.net/callback" })
	void testDynamicClientRegistrationRejectsOtherRedirectHosts(String redirectUri) {
		// GIVEN
		// default allowed hosts; non-loopback hosts must use https

		// WHEN
		sut.run(context -> {
			// THEN
			mockMvc(context).perform(post("/oauth2/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(registrationBody(redirectUri)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_redirect_uri"));
		});
	}

	@Test
	void testAuthorizeRedirectsToGoogleLoginWhenNotAuthenticated() {
		// GIVEN
		// enabled runner, no session

		// WHEN
		sut.run(context -> {
			MockMvc mockMvc = mockMvc(context);
			String clientId = registerClient(mockMvc, CLAUDE_CODE_CALLBACK);

			// THEN
			MvcResult result = mockMvc.perform(authorizeRequest(clientId, CLAUDE_CODE_CALLBACK, MCP_RESOURCE)).andReturn();
			assertThat(result.getResponse().getStatus()).as("authorize response: %s %s", result.getResponse().getErrorMessage(), result.getResponse().getContentAsString())
				.isEqualTo(302);
			assertThat(result.getResponse().getRedirectedUrl()).endsWith("/oauth2/authorization/google");
		});
	}

	@Test
	void testAuthorizationCodeFlowIssuesAudienceBoundTokens() {
		// GIVEN
		// a Google-authenticated user whose module account has roles

		// WHEN
		sut.run(context -> {
			MockMvc mockMvc = mockMvc(context);
			String clientId = registerClient(mockMvc, CLAUDE_CODE_CALLBACK);
			String code = authorize(mockMvc, clientId, CLAUDE_CODE_CALLBACK, MCP_RESOURCE);
			Map<String, Object> tokens = exchangeCode(mockMvc, clientId, CLAUDE_CODE_CALLBACK, code, MCP_RESOURCE);

			// THEN
			assertThat(tokens).containsEntry("token_type", "Bearer").containsKeys("access_token", "refresh_token",
				"expires_in");
			Jwt jwt = context.getBean(JWT_DECODER_BEAN_NAME, JwtDecoder.class).decode((String) tokens.get("access_token"));
			assertThat(jwt.getSubject()).isEqualTo(DEFAULT_EMAIL);
			assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
			assertThat(jwt.getAudience()).containsExactly(MCP_RESOURCE);
			assertThat(jwt.getClaimAsString("email")).isEqualTo(DEFAULT_EMAIL);
			assertThat(jwt.getClaimAsString("name")).isEqualTo(DEFAULT_OAUTH2_NAME);
			assertThat(jwt.getClaimAsString(UserModuleAccessTokenCustomizer.ROLES_CLAIM))
				.isEqualTo(String.join(",", verifiedUser().getAuthorities()));
			var authentication = context.getBean(JWT_AUTHENTICATION_CONVERTER_BEAN_NAME, JwtAuthenticationConverter.class)
				.convert(jwt);
			assertThat(authentication.getName()).isEqualTo(DEFAULT_EMAIL);
			// Spring Security 7 adds its FACTOR_BEARER marker authority on top of the roles claim
			assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsAll(verifiedUser().getAuthorities());

			// refresh tokens are rotated
			Map<String, Object> refreshed = refresh(mockMvc, clientId, (String) tokens.get("refresh_token"), MCP_RESOURCE);
			assertThat(refreshed.get("access_token")).isNotEqualTo(tokens.get("access_token"));
			assertThat(refreshed.get("refresh_token")).isNotNull().isNotEqualTo(tokens.get("refresh_token"));
			Jwt refreshedJwt = context.getBean(JWT_DECODER_BEAN_NAME, JwtDecoder.class)
				.decode((String) refreshed.get("access_token"));
			assertThat(refreshedJwt.getSubject()).isEqualTo(DEFAULT_EMAIL);
			assertThat(refreshedJwt.getAudience()).containsExactly(MCP_RESOURCE);
		});
	}

	@Test
	void testLoopbackRedirectUriMatchesRegardlessOfPort() {
		// GIVEN
		// Claude Code registers one loopback port but may call back on another (RFC 8252)
		String otherPort = "http://localhost:4000/callback";

		// WHEN
		sut.run(context -> {
			MockMvc mockMvc = mockMvc(context);
			String clientId = registerClient(mockMvc, CLAUDE_CODE_CALLBACK);
			String code = authorize(mockMvc, clientId, otherPort, MCP_RESOURCE);

			// THEN
			assertThat(code).isNotBlank();
		});
	}

	@Test
	void testJwtDecoderRejectsTokenIssuedForAnotherResource() {
		// GIVEN
		String otherResource = ISSUER + "/other";

		// WHEN
		sut.run(context -> {
			MockMvc mockMvc = mockMvc(context);
			String clientId = registerClient(mockMvc, CLAUDE_CODE_CALLBACK);
			String code = authorize(mockMvc, clientId, CLAUDE_CODE_CALLBACK, otherResource);
			String accessToken = (String) exchangeCode(mockMvc, clientId, CLAUDE_CODE_CALLBACK, code, otherResource)
				.get("access_token");

			// THEN
			JwtDecoder decoder = context.getBean(JWT_DECODER_BEAN_NAME, JwtDecoder.class);
			assertThatThrownBy(() -> decoder.decode(accessToken)).isInstanceOf(JwtValidationException.class);
		});
	}

	private WebApplicationContextRunner runner(boolean enabled) {
		WebApplicationContextRunner runner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				ServletWebSecurityAutoConfiguration.class,
				OAuth2ClientAutoConfiguration.class,
				OAuth2ClientWebSecurityAutoConfiguration.class,
				OAuth2AuthorizationServerAutoConfiguration.class,
				OAuth2AuthorizationServerJwtAutoConfiguration.class,
				UserConfiguration.class,
				SecurityConfiguration.class,
				OAuth2GoogleConfiguration.class,
				UserModuleAuthorizationServerAutoConfiguration.class))
			.withUserConfiguration(PropertiesConfiguration.class)
			.withBean(UserRepository.class, () -> userRepository)
			.withPropertyValues(
				"spring.security.oauth2.client.registration.google.client-id=test-client-id",
				"spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
				"user.oauth2.authorization-server.issuer=" + ISSUER,
				"user.oauth2.authorization-server.resources=/mcp");
		return enabled ? runner.withPropertyValues("user.oauth2.authorization-server.enabled=true") : runner;
	}

	private static MockMvc mockMvc(AssertableWebApplicationContext context) {
		return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
			.apply(springSecurity())
			.build();
	}

	private static String registerClient(MockMvc mockMvc, String... redirectUris) throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registrationBody(redirectUris)))
			.andReturn();
		String body = result.getResponse().getContentAsString();
		assertThat(result.getResponse().getStatus()).as("registration response: %s", body).isEqualTo(201);
		return JsonPath.read(body, "$.client_id");
	}

	private static String registrationBody(String... redirectUris) {
		return registrationBodyWithScope(null, redirectUris);
	}

	private static String registrationBodyWithScope(String scope, String... redirectUris) {
		String uris = Arrays.stream(redirectUris).map(uri -> "\"" + uri + "\"").collect(Collectors.joining(","));
		return "{\"client_name\":\"Claude Code\",\"redirect_uris\":[" + uris + "],"
			+ "\"grant_types\":[\"authorization_code\",\"refresh_token\"],\"response_types\":[\"code\"],"
			+ "\"token_endpoint_auth_method\":\"none\"" + (scope == null ? "" : ",\"scope\":\"" + scope + "\"") + "}";
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorizeRequest(
		String clientId, String redirectUri, String resource) {
		return get("/oauth2/authorize")
			.accept(MediaType.TEXT_HTML)
			.queryParam("response_type", "code")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("code_challenge", codeChallenge())
			.queryParam("code_challenge_method", "S256")
			.queryParam("resource", resource)
			.queryParam("state", STATE);
	}

	private static String authorize(MockMvc mockMvc, String clientId, String redirectUri, String resource)
		throws Exception {
		MvcResult result = mockMvc.perform(authorizeRequest(clientId, redirectUri, resource)
				.with(authentication(googleAuthentication())))
			.andReturn();
		assertThat(result.getResponse().getStatus()).as("authorize response: %s %s", result.getResponse().getErrorMessage(), result.getResponse().getContentAsString())
			.isEqualTo(302);
		String location = result.getResponse().getRedirectedUrl();
		assertThat(location).startsWith(redirectUri + "?");
		var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
		assertThat(query.getFirst("state")).isEqualTo(STATE);
		return query.getFirst("code");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> exchangeCode(MockMvc mockMvc, String clientId, String redirectUri, String code,
		String resource) throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "authorization_code")
				.param("code", code)
				.param("redirect_uri", redirectUri)
				.param("client_id", clientId)
				.param("code_verifier", CODE_VERIFIER)
				.param("resource", resource))
			.andExpect(status().isOk())
			.andReturn();
		return (Map<String, Object>) JsonPath.read(result.getResponse().getContentAsString(), "$");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> refresh(MockMvc mockMvc, String clientId, String refreshToken, String resource)
		throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "refresh_token")
				.param("refresh_token", refreshToken)
				.param("client_id", clientId)
				.param("resource", resource))
			.andExpect(status().isOk())
			.andReturn();
		return (Map<String, Object>) JsonPath.read(result.getResponse().getContentAsString(), "$");
	}

	private static String codeChallenge() {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static OAuth2AuthenticationToken googleAuthentication() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", "google-subject");
		attributes.put("email", DEFAULT_EMAIL);
		attributes.put("email_verified", true);
		attributes.put("name", DEFAULT_OAUTH2_NAME);
		OAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes,
			"sub");
		return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(UserProperties.class)
	static class PropertiesConfiguration {
	}
}
