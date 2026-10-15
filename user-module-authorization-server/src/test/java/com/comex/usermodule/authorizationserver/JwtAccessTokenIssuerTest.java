package com.comex.usermodule.authorizationserver;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

class JwtAccessTokenIssuerTest {

	private static final String ISSUER = "http://localhost:8081";
	private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

	private RSAKey rsaKey;
	private JwtAccessTokenIssuer sut;

	@BeforeEach
	void setUp() {
		rsaKey = RsaJwkFactory.create(new AuthorizationServerProperties.Jwk());
		sut = new JwtAccessTokenIssuer(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))),
			ProtectedResources.of(ISSUER, List.of("/mcp", "/api"), List.of()), Duration.ofHours(1),
			Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void testIssue() throws Exception {
		// GIVEN
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
		decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());

		// WHEN
		String token = sut.issue(verifiedUser());

		// THEN
		Jwt jwt = decoder.decode(token);
		assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
		assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
		assertThat(jwt.getSubject()).isEqualTo(DEFAULT_EMAIL);
		assertThat(jwt.getClaimAsString("email")).isEqualTo(DEFAULT_EMAIL);
		assertThat(jwt.getAudience()).containsExactly(ISSUER + "/mcp", ISSUER + "/api");
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
		assertThat(jwt.getId()).isNotBlank();
		assertThat(jwt.getClaimAsString(UserModuleAccessTokenCustomizer.ROLES_CLAIM).split(","))
			.containsExactlyInAnyOrderElementsOf(verifiedUser().getAuthorities());
	}
}
