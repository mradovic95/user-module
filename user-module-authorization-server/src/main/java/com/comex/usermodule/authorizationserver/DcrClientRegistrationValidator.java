package com.comex.usermodule.authorizationserver;

import static org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR;
import static org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationToken;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates Dynamic Client Registration requests: redirect URIs are required, must be well-formed and must point at
 * an allowed host. Loopback hosts may use any port and plain http; any other allowed host must use https.
 * <p>
 * Unlike Spring's default validator, a {@code scope} in the registration request is accepted and ignored, because
 * some MCP clients send one.
 */
@Slf4j
public final class DcrClientRegistrationValidator implements Consumer<OAuth2ClientRegistrationAuthenticationContext> {

	static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.2";
	private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

	private final Set<String> allowedHosts;
	private final Consumer<OAuth2ClientRegistrationAuthenticationContext> springValidators = DEFAULT_REDIRECT_URI_VALIDATOR
		.andThen(DEFAULT_JWK_SET_URI_VALIDATOR);

	public DcrClientRegistrationValidator(Collection<String> allowedHosts) {
		this.allowedHosts = allowedHosts == null ? Set.of() : allowedHosts.stream()
			.map(host -> host.trim().toLowerCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public void accept(OAuth2ClientRegistrationAuthenticationContext context) {
		springValidators.accept(context);
		OAuth2ClientRegistrationAuthenticationToken authentication = context.getAuthentication();
		List<String> redirectUris = authentication.getClientRegistration().getRedirectUris();
		if (CollectionUtils.isEmpty(redirectUris)) {
			throw invalidRedirectUri("redirect_uris is required");
		}
		for (String redirectUri : redirectUris) {
			URI uri = URI.create(redirectUri);
			String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
			if (host == null || !allowedHosts.contains(host)) {
				log.warn("Dynamic client registration rejected: redirect host of '{}' is not allowed.", redirectUri);
				throw invalidRedirectUri("redirect_uri host is not allowed");
			}
			if (!LOOPBACK_HOSTS.contains(host) && !"https".equalsIgnoreCase(uri.getScheme())) {
				log.warn("Dynamic client registration rejected: redirect '{}' must use https.", redirectUri);
				throw invalidRedirectUri("redirect_uri must use https");
			}
		}
	}

	private static OAuth2AuthenticationException invalidRedirectUri(String description) {
		return new OAuth2AuthenticationException(
			new OAuth2Error(OAuth2ErrorCodes.INVALID_REDIRECT_URI, "Invalid Client Registration: " + description,
				ERROR_URI));
	}
}
