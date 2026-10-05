package com.comex.usermodule.authorizationserver.resource;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.RequiredArgsConstructor;

/**
 * Accepts a token only when its {@code aud} names one of the configured protected resources (RFC 8707 audience
 * binding).
 */
@RequiredArgsConstructor
public class ResourceAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
		"The token audience does not match this resource", null);

	private final ProtectedResources protectedResources;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		boolean matches = token.getAudience() != null
			&& token.getAudience().stream().anyMatch(protectedResources::isAudience);
		return matches ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
	}
}
