package com.comex.usermodule.authorizationserver;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_OAUTH2_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs the module's Liquibase changelog against PostgreSQL and round-trips a registered client, an authorization
 * (with a Google {@link OAuth2AuthenticationToken} principal) and a consent through the JDBC implementations.
 */
@Testcontainers
class JdbcPersistenceIntegrationTest {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
		.withDatabaseName("testdb")
		.withUsername("test")
		.withPassword("test");

	private final ApplicationContextRunner sut = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			DataSourceAutoConfiguration.class,
			JdbcTemplateAutoConfiguration.class,
			LiquibaseAutoConfiguration.class,
			JdbcPersistenceAutoConfiguration.class))
		.withPropertyValues(
			"spring.datasource.url=" + postgres.getJdbcUrl(),
			"spring.datasource.username=" + postgres.getUsername(),
			"spring.datasource.password=" + postgres.getPassword(),
			"spring.liquibase.change-log=classpath:db/changelog/user-master.yml");

	@Test
	void testRegisteredClientAuthorizationAndConsentRoundTrip() {
		// GIVEN
		RegisteredClient client = dynamicallyRegisteredClient();
		OAuth2AuthenticationToken googleUser = googleAuthentication();

		// WHEN
		sut.run(context -> {
			RegisteredClientRepository clients = context.getBean(RegisteredClientRepository.class);
			OAuth2AuthorizationService authorizations = context.getBean(OAuth2AuthorizationService.class);
			OAuth2AuthorizationConsentService consents = context.getBean(OAuth2AuthorizationConsentService.class);
			assertThat(clients).isInstanceOf(JdbcRegisteredClientRepository.class);

			clients.save(client);
			Instant issuedAt = Instant.now();
			OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
				.id(UUID.randomUUID().toString())
				.principalName(googleUser.getName())
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizedScopes(Set.of())
				.attribute(Principal.class.getName(), googleUser)
				.accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token-value",
					issuedAt, issuedAt.plus(Duration.ofHours(1))))
				.refreshToken(new OAuth2RefreshToken("refresh-token-value", issuedAt,
					issuedAt.plus(Duration.ofDays(30))))
				.build();
			authorizations.save(authorization);
			consents.save(OAuth2AuthorizationConsent.withId(client.getId(), googleUser.getName())
				.scope("tools")
				.build());

			// THEN
			RegisteredClient storedClient = clients.findByClientId(client.getClientId());
			assertThat(storedClient).isNotNull();
			assertThat(storedClient.getRedirectUris()).containsExactly("http://localhost:3118/callback");
			assertThat(storedClient.getClientSettings().isRequireProofKey()).isTrue();
			assertThat(storedClient.getTokenSettings().isReuseRefreshTokens()).isFalse();

			OAuth2Authorization stored = authorizations.findByToken("access-token-value", OAuth2TokenType.ACCESS_TOKEN);
			assertThat(stored).isNotNull();
			assertThat(stored.getPrincipalName()).isEqualTo(googleUser.getName());
			assertThat(stored.getRefreshToken()).isNotNull();
			OAuth2AuthenticationToken principal = stored.getAttribute(Principal.class.getName());
			assertThat(principal).isNotNull();
			assertThat(principal.getPrincipal().<String>getAttribute("email")).isEqualTo(DEFAULT_EMAIL);

			OAuth2AuthorizationConsent storedConsent = consents.findById(client.getId(), googleUser.getName());
			assertThat(storedConsent).isNotNull();
			assertThat(storedConsent.getScopes()).containsExactly("tools");
		});
	}

	/** Stands in for the module auto-configuration, which orders itself after the JDBC auto-configurations. */
	@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
	@Import(UserModuleAuthorizationServerAutoConfiguration.JdbcPersistenceConfiguration.class)
	static class JdbcPersistenceAutoConfiguration {
	}

	private static RegisteredClient dynamicallyRegisteredClient() {
		return RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId("claude-code-" + UUID.randomUUID())
			.clientName("Claude Code")
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.redirectUri("http://localhost:3118/callback")
			.clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
			.tokenSettings(TokenSettings.builder().reuseRefreshTokens(false).build())
			.build();
	}

	private static OAuth2AuthenticationToken googleAuthentication() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", "google-subject");
		attributes.put("email", DEFAULT_EMAIL);
		attributes.put("name", DEFAULT_OAUTH2_NAME);
		OAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "sub");
		return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
	}
}
