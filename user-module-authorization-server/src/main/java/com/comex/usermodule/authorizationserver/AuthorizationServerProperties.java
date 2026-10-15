package com.comex.usermodule.authorizationserver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Settings of the module's OAuth 2.1 authorization server, prefix {@value #PREFIX}.
 */
@Data
@ConfigurationProperties(prefix = AuthorizationServerProperties.PREFIX)
public class AuthorizationServerProperties {

	public static final String PREFIX = "user.oauth2.authorization-server";

	/**
	 * Public base URL of this application, e.g. {@code https://api.example.com} or {@code http://localhost:8081}.
	 * Used as the token issuer and as the base of every protected resource URI. Required.
	 */
	private String issuer;

	/**
	 * Paths (relative to the issuer) of the protected resources this server issues tokens for, e.g. {@code /mcp}.
	 * Each one gets a protected resource metadata document, and every issued token carries all of them as {@code aud}.
	 */
	private List<String> resources = new ArrayList<>();

	/** Scopes advertised to clients. Empty means no scopes and therefore no consent screen. */
	private List<String> scopesSupported = new ArrayList<>();

	/**
	 * Hosts a dynamically registered client may use in its redirect URIs. Loopback hosts may use any port and
	 * plain http; every other host must use https.
	 */
	private List<String> allowedRedirectHosts = new ArrayList<>(List.of("localhost", "127.0.0.1", "claude.ai"));

	/** Lifetime of issued access tokens. */
	private Duration accessTokenTtl = Duration.ofHours(1);

	/** Lifetime of issued refresh tokens. Refresh tokens are always rotated. */
	private Duration refreshTokenTtl = Duration.ofDays(30);

	/**
	 * Whether dynamically registered clients must get the user's consent. The consent page is only shown when the
	 * client requests at least one scope.
	 */
	private boolean consentRequired = true;

	/** Path the browser is sent to when a user must log in; the module's Google login by default. */
	private String loginPath = "/oauth2/authorization/google";

	/**
	 * Client id of the application's own client (e.g. its frontend), registered at startup as a public client allowed
	 * the {@code password} grant on {@code /oauth2/token}. Blank means no first-party client and no password grant.
	 */
	private String firstPartyClientId;

	private Jwk jwk = new Jwk();

	@Data
	public static class Jwk {

		/**
		 * RSA private key (PKCS#8 PEM, with or without the BEGIN/END lines) used to sign tokens. When blank a key
		 * is generated at startup, which invalidates every issued token on restart.
		 */
		private String privateKeyPem;

		/** Key id published in the JWK set; derived from the key when blank. */
		private String kid;
	}
}
