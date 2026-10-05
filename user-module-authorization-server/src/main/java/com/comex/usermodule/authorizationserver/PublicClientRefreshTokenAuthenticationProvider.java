package com.comex.usermodule.authorizationserver;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates a public client on a {@code refresh_token} request by its {@code client_id}; the refresh token
 * itself is verified by Spring's refresh-token grant provider afterwards. Must run before Spring's
 * {@code PublicClientAuthenticationProvider}, which would demand a PKCE {@code code_verifier}.
 */
@Slf4j
@RequiredArgsConstructor
public final class PublicClientRefreshTokenAuthenticationProvider implements AuthenticationProvider {

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";

	private final RegisteredClientRepository registeredClientRepository;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;
		if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
			return null;
		}
		Object grantType = clientAuthentication.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE);
		if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
			// PKCE code exchanges are handled by Spring's PublicClientAuthenticationProvider
			return null;
		}
		String clientId = clientAuthentication.getPrincipal().toString();
		RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
		if (registeredClient == null) {
			throw invalidClient(OAuth2ParameterNames.CLIENT_ID);
		}
		if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
			throw invalidClient("authentication_method");
		}
		log.debug("Authenticated public client {} for a refresh token request.", clientId);
		return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private static OAuth2AuthenticationException invalidClient(String parameterName) {
		return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT,
			"Client authentication failed: " + parameterName, ERROR_URI));
	}
}
