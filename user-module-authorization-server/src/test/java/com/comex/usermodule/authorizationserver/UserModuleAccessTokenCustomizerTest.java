package com.comex.usermodule.authorizationserver;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_OAUTH2_NAME;
import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.core.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserModuleAccessTokenCustomizerTest {

	private static final String ISSUER = "http://localhost:8081";
	private static final String MCP_RESOURCE = ISSUER + "/mcp";

	@Mock
	private UserService userService;

	private UserModuleAccessTokenCustomizer sut;

	@BeforeEach
	void setUp() {
		sut = new UserModuleAccessTokenCustomizer(userService,
			ProtectedResources.of(ISSUER, List.of("/mcp"), List.of()));
	}

	@Test
	void testCustomizeAccessTokenForGoogleUser() {
		// GIVEN
		when(userService.findByEmailOptional(DEFAULT_EMAIL)).thenReturn(Optional.of(verifiedUser()));
		JwtEncodingContext context = context(OAuth2TokenType.ACCESS_TOKEN, googleAuthentication(DEFAULT_EMAIL));

		// WHEN
		sut.customize(context);

		// THEN
		Map<String, Object> claims = context.getClaims().build().getClaims();
		assertThat(claims).containsEntry("sub", DEFAULT_EMAIL)
			.containsEntry("email", DEFAULT_EMAIL)
			.containsEntry("name", DEFAULT_OAUTH2_NAME)
			.containsEntry(UserModuleAccessTokenCustomizer.ROLES_CLAIM,
				String.join(",", verifiedUser().getAuthorities()))
			.containsEntry("aud", List.of(MCP_RESOURCE));
	}

	@Test
	void testCustomizeKeepsAudienceSetByResourceParameter() {
		// GIVEN
		when(userService.findByEmailOptional(DEFAULT_EMAIL)).thenReturn(Optional.empty());
		JwtEncodingContext context = context(OAuth2TokenType.ACCESS_TOKEN, googleAuthentication(DEFAULT_EMAIL));
		context.getClaims().audience(List.of("http://localhost:8081/other"));

		// WHEN
		sut.customize(context);

		// THEN
		Map<String, Object> claims = context.getClaims().build().getClaims();
		assertThat(claims).containsEntry("aud", List.of("http://localhost:8081/other"))
			.containsEntry(UserModuleAccessTokenCustomizer.ROLES_CLAIM, "");
	}

	@Test
	void testCustomizeIgnoresRefreshTokens() {
		// GIVEN
		JwtEncodingContext context = context(OAuth2TokenType.REFRESH_TOKEN, googleAuthentication(DEFAULT_EMAIL));

		// WHEN
		sut.customize(context);

		// THEN
		assertThat(context.getClaims().build().getClaims()).containsEntry("sub", "google-subject")
			.doesNotContainKeys("email", UserModuleAccessTokenCustomizer.ROLES_CLAIM);
		verifyNoInteractions(userService);
	}

	@Test
	void testCustomizeIgnoresPrincipalsWithoutEmail() {
		// GIVEN
		Authentication principal = new UsernamePasswordAuthenticationToken("service-account", null, List.of());
		JwtEncodingContext context = context(OAuth2TokenType.ACCESS_TOKEN, principal);

		// WHEN
		sut.customize(context);

		// THEN
		assertThat(context.getClaims().build().getClaims()).containsEntry("sub", "google-subject")
			.doesNotContainKeys("email", UserModuleAccessTokenCustomizer.ROLES_CLAIM);
		verifyNoInteractions(userService);
	}

	private static JwtEncodingContext context(OAuth2TokenType tokenType, Authentication principal) {
		return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256),
				JwtClaimsSet.builder().subject("google-subject"))
			.tokenType(tokenType)
			.principal(principal)
			.build();
	}

	private static OAuth2AuthenticationToken googleAuthentication(String email) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", "google-subject");
		attributes.put("email", email);
		attributes.put("name", DEFAULT_OAUTH2_NAME);
		OAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "sub");
		return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
	}
}
