package com.comex.usermodule.authorizationserver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springaicommunity.mcp.security.authorizationserver.config.LocalhostWildcardPortValidator;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerJwtAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.McpDefaultJwtCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.mcp.token.ResourceIdentifierAudienceTokenCustomizer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.comex.usermodule.authorizationserver.resource.BearerResourceMetadataEntryPoint;
import com.comex.usermodule.authorizationserver.resource.ProtectedResourceMetadataFilter;
import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.authorizationserver.resource.ResourceAudienceValidator;
import com.comex.usermodule.core.service.UserService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * OAuth 2.1 authorization server for MCP clients (Claude Code, claude.ai, MCP Inspector, ...), switched on with
 * {@code user.oauth2.authorization-server.enabled=true}.
 * <p>
 * Registers its own {@link SecurityFilterChain} for the authorization server endpoints and the protected resource
 * metadata. Users log in through the module's Google login chain: an unauthenticated {@code /oauth2/authorize} is
 * saved in the session, the browser is sent to Google, and the login success handler resumes it. Issued access
 * tokens carry {@code sub}=email and {@code roles} like the module's own JWTs, and {@code aud} bound to the
 * configured protected resources. Clients register through Dynamic Client Registration (PKCE enforced, redirect
 * hosts allow-listed, refresh tokens rotated).
 * <p>
 * For applications protecting a resource with these tokens it also provides a {@link JwtDecoder}
 * ({@value #JWT_DECODER_BEAN_NAME}), a {@link JwtAuthenticationConverter} mapping the {@code roles} claim
 * ({@value #JWT_AUTHENTICATION_CONVERTER_BEAN_NAME}) and the {@code 401} entry point
 * ({@value #BEARER_ENTRY_POINT_BEAN_NAME}) that points clients at the metadata.
 */
@AutoConfiguration(
	before = { OAuth2AuthorizationServerAutoConfiguration.class, OAuth2AuthorizationServerJwtAutoConfiguration.class },
	afterName = { "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration" })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = AuthorizationServerProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AuthorizationServerProperties.class)
@Import({ UserModuleAuthorizationServerAutoConfiguration.JdbcPersistenceConfiguration.class,
	UserModuleAuthorizationServerAutoConfiguration.InMemoryPersistenceConfiguration.class })
public class UserModuleAuthorizationServerAutoConfiguration {

	/** Bean name of the authorization server chain. */
	public static final String AUTHORIZATION_SERVER_CHAIN_BEAN_NAME = "userModuleAuthorizationServerSecurityFilterChain";

	/** Runs before the Google login chain and before any application chain. */
	public static final int AUTHORIZATION_SERVER_CHAIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	public static final String JWT_DECODER_BEAN_NAME = "userModuleAuthorizationServerJwtDecoder";
	public static final String JWT_AUTHENTICATION_CONVERTER_BEAN_NAME = "userModuleJwtAuthenticationConverter";
	public static final String BEARER_ENTRY_POINT_BEAN_NAME = "userModuleBearerResourceMetadataEntryPoint";

	private static final String OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";

	@ConditionalOnMissingBean
	@Bean
	public ProtectedResources userModuleProtectedResources(AuthorizationServerProperties properties) {
		Assert.hasText(properties.getIssuer(), AuthorizationServerProperties.PREFIX + ".issuer must be set");
		return ProtectedResources.of(properties.getIssuer(), properties.getResources(),
			properties.getScopesSupported());
	}

	@ConditionalOnMissingBean
	@Bean
	public AuthorizationServerSettings authorizationServerSettings(ProtectedResources protectedResources) {
		return AuthorizationServerSettings.builder().issuer(protectedResources.issuer()).build();
	}

	@ConditionalOnMissingBean(JWKSource.class)
	@Bean
	public JWKSource<SecurityContext> userModuleJwkSource(AuthorizationServerProperties properties) {
		RSAKey rsaKey = RsaJwkFactory.create(properties.getJwk());
		return new ImmutableJWKSet<>(new JWKSet(rsaKey));
	}

	@ConditionalOnMissingBean
	@Bean
	public UserModuleAccessTokenCustomizer userModuleAccessTokenCustomizer(UserService userService,
		ProtectedResources protectedResources) {
		return new UserModuleAccessTokenCustomizer(userService, protectedResources);
	}

	/**
	 * Signed JWT access tokens (with the MCP {@code resource} parameter as {@code aud}) plus refresh tokens for every
	 * client, public ones included. Replaces the generator the MCP configurer would build, which inherits Spring's
	 * refusal to issue refresh tokens to public clients.
	 */
	@ConditionalOnMissingBean(OAuth2TokenGenerator.class)
	@Bean
	public OAuth2TokenGenerator<? extends OAuth2Token> userModuleTokenGenerator(JWKSource<SecurityContext> jwkSource,
		ObjectProvider<OAuth2TokenCustomizer<JwtEncodingContext>> jwtCustomizers) {
		List<OAuth2TokenCustomizer<JwtEncodingContext>> customizers = jwtCustomizers.orderedStream().toList();
		ResourceIdentifierAudienceTokenCustomizer audienceCustomizer = new ResourceIdentifierAudienceTokenCustomizer();
		JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
		jwtGenerator.setJwtCustomizer(context -> {
			McpDefaultJwtCustomizer.DEFAULT_JWT_CUSTOMIZER.customize(context);
			audienceCustomizer.customize(context);
			customizers.forEach(customizer -> customizer.customize(context));
		});
		return new DelegatingOAuth2TokenGenerator(jwtGenerator, new PublicClientRefreshTokenGenerator());
	}

	/**
	 * Verifies tokens issued by this server in-process (no HTTP round trip to the JWK set): signature, issuer and
	 * audience against the configured protected resources.
	 */
	@Bean(JWT_DECODER_BEAN_NAME)
	public JwtDecoder userModuleAuthorizationServerJwtDecoder(JWKSource<SecurityContext> jwkSource,
		AuthorizationServerSettings authorizationServerSettings, ProtectedResources protectedResources) {
		DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
		jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
		});
		NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(jwtProcessor);
		jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
			JwtValidators.createDefaultWithIssuer(authorizationServerSettings.getIssuer()),
			new ResourceAudienceValidator(protectedResources)));
		return jwtDecoder;
	}

	/** Authenticates a decoded token with name = {@code sub} (email) and authorities from the {@code roles} claim. */
	@Bean(JWT_AUTHENTICATION_CONVERTER_BEAN_NAME)
	public JwtAuthenticationConverter userModuleJwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(UserModuleAuthorizationServerAutoConfiguration::rolesClaimAuthorities);
		return converter;
	}

	/** {@code 401} challenge for the default (first configured) protected resource. */
	@ConditionalOnMissingBean(name = BEARER_ENTRY_POINT_BEAN_NAME)
	@Bean(BEARER_ENTRY_POINT_BEAN_NAME)
	public BearerResourceMetadataEntryPoint userModuleBearerResourceMetadataEntryPoint(
		ProtectedResources protectedResources) {
		return protectedResources.entryPoint(protectedResources.defaultPath());
	}

	@Bean(AUTHORIZATION_SERVER_CHAIN_BEAN_NAME)
	@Order(AUTHORIZATION_SERVER_CHAIN_ORDER)
	public SecurityFilterChain userModuleAuthorizationServerSecurityFilterChain(HttpSecurity http,
		AuthorizationServerProperties properties, AuthorizationServerSettings authorizationServerSettings,
		RegisteredClientRepository registeredClientRepository, OAuth2AuthorizationService authorizationService,
		OAuth2AuthorizationConsentService authorizationConsentService, ProtectedResources protectedResources,
		ObjectProvider<CorsConfigurationSource> corsConfigurationSource) throws Exception {
		RequestMatcher metadataMatcher = ProtectedResourceMetadataFilter.requestMatcher();
		RequestMatcher openIdConfigurationMatcher = PathPatternRequestMatcher.withDefaults()
			.matcher(OPENID_CONFIGURATION_PATH);
		UserModuleRegisteredClientConverter registeredClientConverter = new UserModuleRegisteredClientConverter(
			properties);

		http.with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), mcp -> {
			mcp.dynamicClientRegistration(true);
			mcp.dynamicClientRegistrationValidator(
				new DcrClientRegistrationValidator(properties.getAllowedRedirectHosts()));
			// loopback redirect URIs may use any port (RFC 8252); keep Spring's scope validation
			mcp.authorizationCodeRequestValidator(new LocalhostWildcardPortValidator()
				.andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR));
			mcp.authorizationServer(authorizationServer -> {
				http.securityMatcher(new OrRequestMatcher(authorizationServer.getEndpointsMatcher(),
					openIdConfigurationMatcher, metadataMatcher));
				authorizationServer
					.authorizationServerSettings(authorizationServerSettings)
					.registeredClientRepository(registeredClientRepository)
					.authorizationService(authorizationService)
					.authorizationConsentService(authorizationConsentService)
					// public clients must be able to refresh; Spring only authenticates them on the PKCE exchange
					.clientAuthentication(clientAuthentication -> clientAuthentication
						.authenticationConverters(converters -> converters
							.add(new PublicClientRefreshTokenAuthenticationConverter()))
						.authenticationProviders(providers -> providers
							.add(0, new PublicClientRefreshTokenAuthenticationProvider(registeredClientRepository))))
					.clientRegistrationEndpoint(endpoint -> endpoint.authenticationProviders(providers -> providers
						.stream()
						.filter(OAuth2ClientRegistrationAuthenticationProvider.class::isInstance)
						.map(OAuth2ClientRegistrationAuthenticationProvider.class::cast)
						.forEach(provider -> provider.setRegisteredClientConverter(registeredClientConverter))))
					.authorizationServerMetadataEndpoint(endpoint -> endpoint.authorizationServerMetadataCustomizer(
						metadata -> {
							// public clients (PKCE, no secret) are what MCP clients register as
							metadata.tokenEndpointAuthenticationMethod(ClientAuthenticationMethod.NONE.getValue());
							properties.getScopesSupported().forEach(metadata::scope);
						}));
			});
		});

		// browser-based MCP clients (e.g. the MCP Inspector) call the OAuth endpoints cross-origin; honour the
		// application's CORS policy when it has one
		CorsConfigurationSource cors = corsConfigurationSource.getIfAvailable();
		if (cors != null) {
			http.cors(customizer -> customizer.configurationSource(cors));
		}
		return http
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(metadataMatcher).permitAll()
				.anyRequest().authenticated())
			// right after CORS (browser clients fetch the metadata cross-origin) and ahead of Spring Security's own
			// OAuth2ProtectedResourceMetadataFilter, which knows nothing about our authorization server
			.addFilterAfter(new ProtectedResourceMetadataFilter(protectedResources), CorsFilter.class)
			.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
				new LoginUrlAuthenticationEntryPoint(properties.getLoginPath()), htmlRequestMatcher()))
			.build();
	}

	static Collection<GrantedAuthority> rolesClaimAuthorities(Jwt jwt) {
		String roles = jwt.getClaimAsString(UserModuleAccessTokenCustomizer.ROLES_CLAIM);
		if (!StringUtils.hasText(roles)) {
			return List.of();
		}
		return Arrays.stream(roles.split(","))
			.map(String::trim)
			.filter(StringUtils::hasText)
			.<GrantedAuthority>map(SimpleGrantedAuthority::new)
			.toList();
	}

	private static RequestMatcher htmlRequestMatcher() {
		MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
		matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
		return matcher;
	}

	/** Registered clients, authorizations and consents in the application's database (tables via Liquibase). */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcOperations.class)
	@ConditionalOnBean(JdbcOperations.class)
	static class JdbcPersistenceConfiguration {

		@ConditionalOnMissingBean
		@Bean
		RegisteredClientRepository registeredClientRepository(JdbcOperations jdbcOperations) {
			return new JdbcRegisteredClientRepository(jdbcOperations);
		}

		@ConditionalOnMissingBean
		@Bean
		OAuth2AuthorizationService oAuth2AuthorizationService(JdbcOperations jdbcOperations,
			RegisteredClientRepository registeredClientRepository) {
			return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
		}

		@ConditionalOnMissingBean
		@Bean
		OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService(JdbcOperations jdbcOperations,
			RegisteredClientRepository registeredClientRepository) {
			return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
		}
	}

	/** Fallback without a JDBC {@code DataSource}: everything is lost on restart. */
	@Configuration(proxyBeanMethods = false)
	static class InMemoryPersistenceConfiguration {

		/** The in-memory repository refuses to start empty; this client cannot be used for anything. */
		private static final RegisteredClient PLACEHOLDER_CLIENT = RegisteredClient.withId("user-module-placeholder")
			.clientId("user-module-placeholder")
			.clientName("user-module placeholder (unusable)")
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.build();

		@ConditionalOnMissingBean
		@Bean
		RegisteredClientRepository registeredClientRepository() {
			return new InMemoryRegisteredClientRepository(PLACEHOLDER_CLIENT);
		}

		@ConditionalOnMissingBean
		@Bean
		OAuth2AuthorizationService oAuth2AuthorizationService() {
			return new InMemoryOAuth2AuthorizationService();
		}

		@ConditionalOnMissingBean
		@Bean
		OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService() {
			return new InMemoryOAuth2AuthorizationConsentService();
		}
	}
}
