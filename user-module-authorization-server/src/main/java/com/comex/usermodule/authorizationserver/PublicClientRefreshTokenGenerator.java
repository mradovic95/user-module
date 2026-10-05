package com.comex.usermodule.authorizationserver;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * Issues refresh tokens to every client that has the {@code refresh_token} grant, including public clients.
 * <p>
 * Spring's {@code OAuth2RefreshTokenGenerator} refuses refresh tokens to public clients on the authorization code
 * grant. MCP clients (Claude Code, claude.ai) are public PKCE clients that rely on refresh tokens, and OAuth 2.1
 * permits it as long as the tokens are rotated, which {@link UserModuleRegisteredClientConverter} enforces.
 */
public final class PublicClientRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

	private final StringKeyGenerator refreshTokenGenerator = new Base64StringKeyGenerator(
		Base64.getUrlEncoder().withoutPadding(), 96);
	private final Clock clock;

	public PublicClientRefreshTokenGenerator() {
		this(Clock.systemUTC());
	}

	PublicClientRefreshTokenGenerator(Clock clock) {
		this.clock = clock;
	}

	@Override
	public OAuth2RefreshToken generate(OAuth2TokenContext context) {
		if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
			return null;
		}
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
		return new OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
	}
}
