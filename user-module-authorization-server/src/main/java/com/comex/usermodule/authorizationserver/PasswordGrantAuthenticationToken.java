package com.comex.usermodule.authorizationserver;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

/**
 * A {@code grant_type=password} token request: the user's email and password, exchanged on {@code /oauth2/token} by
 * a first-party client for the same access token every other flow of the module issues.
 */
public final class PasswordGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

	/** OAuth 2.1 dropped the resource owner password grant; it is kept here for first-party clients only. */
	public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

	private final String username;
	private final String password;
	private final Set<String> scopes;

	public PasswordGrantAuthenticationToken(String username, String password, Set<String> scopes,
		Authentication clientPrincipal, Map<String, Object> additionalParameters) {
		super(PASSWORD, clientPrincipal, additionalParameters);
		this.username = username;
		this.password = password;
		this.scopes = Set.copyOf(scopes);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Set<String> getScopes() {
		return scopes;
	}

	@Override
	public Object getCredentials() {
		return "";
	}
}
