package com.comex.usermodule.authorizationserver;

import java.security.Principal;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exchanges a user's email and password for an access token (plus a rotated refresh token) through the same token
 * generator as every other grant, so the token has the same claims, audience and lifetime. The password is checked
 * by the module's {@link AuthenticationManager} (BCrypt). Only clients registered with the {@code password} grant may
 * use it; dynamically registered (MCP) clients never are.
 */
@Slf4j
@RequiredArgsConstructor
public final class PasswordGrantAuthenticationProvider implements AuthenticationProvider {

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

	private final AuthenticationManager authenticationManager;
	private final OAuth2AuthorizationService authorizationService;
	private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		PasswordGrantAuthenticationToken passwordGrant = (PasswordGrantAuthenticationToken) authentication;
		OAuth2ClientAuthenticationToken clientPrincipal = authenticatedClient(passwordGrant);
		RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
		if (registeredClient == null
			|| !registeredClient.getAuthorizationGrantTypes().contains(PasswordGrantAuthenticationToken.PASSWORD)) {
			throw error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "The client may not use the password grant");
		}
		Set<String> scopes = passwordGrant.getScopes();
		if (!registeredClient.getScopes().containsAll(scopes)) {
			throw error(OAuth2ErrorCodes.INVALID_SCOPE, "OAuth 2.0 Parameter: scope");
		}

		Authentication user;
		try {
			Authentication checked = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(passwordGrant.getUsername(), passwordGrant.getPassword()));
			// only the email and authorities are kept: the principal is persisted with the authorization
			user = UsernamePasswordAuthenticationToken.authenticated(checked.getName(), null,
				checked.getAuthorities());
		} catch (AuthenticationException e) {
			log.info("Password grant rejected for {}: {}", passwordGrant.getUsername(), e.getMessage());
			throw error(OAuth2ErrorCodes.INVALID_GRANT, "Bad credentials");
		}

		DefaultOAuth2TokenContext.Builder tokenContext = DefaultOAuth2TokenContext.builder()
			.registeredClient(registeredClient)
			.principal(user)
			.authorizationServerContext(AuthorizationServerContextHolder.getContext())
			.authorizedScopes(scopes)
			.authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
			.authorizationGrant(passwordGrant);
		OAuth2Authorization.Builder authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
			.principalName(user.getName())
			.authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
			.authorizedScopes(scopes)
			.attribute(Principal.class.getName(), user);

		OAuth2Token generatedAccessToken = generate(tokenContext, OAuth2TokenType.ACCESS_TOKEN);
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
			generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
			generatedAccessToken.getExpiresAt(), scopes);
		if (generatedAccessToken instanceof ClaimAccessor claims) {
			authorization.token(accessToken, metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
				claims.getClaims()));
		} else {
			authorization.accessToken(accessToken);
		}

		OAuth2RefreshToken refreshToken = null;
		if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
			refreshToken = (OAuth2RefreshToken) generate(tokenContext, OAuth2TokenType.REFRESH_TOKEN);
			authorization.refreshToken(refreshToken);
		}

		authorizationService.save(authorization.build());
		log.debug("Issued tokens to {} through the password grant of client {}.", user.getName(),
			registeredClient.getClientId());
		return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return PasswordGrantAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private OAuth2Token generate(DefaultOAuth2TokenContext.Builder tokenContext, OAuth2TokenType tokenType) {
		OAuth2TokenContext context = tokenContext.tokenType(tokenType).build();
		OAuth2Token token = tokenGenerator.generate(context);
		if (token == null) {
			throw error(OAuth2ErrorCodes.SERVER_ERROR,
				"The token generator failed to generate the " + tokenType.getValue());
		}
		return token;
	}

	private static OAuth2ClientAuthenticationToken authenticatedClient(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken client && client.isAuthenticated()) {
			return client;
		}
		throw error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed");
	}

	private static OAuth2AuthenticationException error(String errorCode, String description) {
		return new OAuth2AuthenticationException(new OAuth2Error(errorCode, description, ERROR_URI));
	}
}
