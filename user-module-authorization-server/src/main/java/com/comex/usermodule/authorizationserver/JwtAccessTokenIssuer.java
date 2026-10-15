package com.comex.usermodule.authorizationserver;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.port.AccessTokenIssuer;

import lombok.extern.slf4j.Slf4j;

/**
 * Issues the access token handed out by {@code POST /user/login} and the Google login redirect. It is signed with the
 * authorization server's key and carries the same claims as a token from {@code /oauth2/token} ({@code iss},
 * {@code sub}=email, {@code email}, {@code roles}, {@code aud}=every protected resource), so one resource server
 * configuration accepts every token the module issues.
 */
@Slf4j
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final ProtectedResources protectedResources;
	private final Duration accessTokenTtl;
	private final Clock clock;

	public JwtAccessTokenIssuer(JwtEncoder jwtEncoder, ProtectedResources protectedResources,
		Duration accessTokenTtl) {
		this(jwtEncoder, protectedResources, accessTokenTtl, Clock.systemUTC());
	}

	JwtAccessTokenIssuer(JwtEncoder jwtEncoder, ProtectedResources protectedResources, Duration accessTokenTtl,
		Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.protectedResources = protectedResources;
		this.accessTokenTtl = accessTokenTtl;
		this.clock = clock;
	}

	@Override
	public String issue(User user) {
		log.info("Issuing access token for user: {}.", user.getEmail());
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(protectedResources.issuer())
			.subject(user.getEmail())
			.audience(protectedResources.resourceUris())
			.issuedAt(issuedAt)
			.notBefore(issuedAt)
			.expiresAt(issuedAt.plus(accessTokenTtl))
			.id(UUID.randomUUID().toString())
			.claim(StandardClaimNames.EMAIL, user.getEmail())
			.claim(UserModuleAccessTokenCustomizer.ROLES_CLAIM, String.join(",", user.getAuthorities()))
			.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
