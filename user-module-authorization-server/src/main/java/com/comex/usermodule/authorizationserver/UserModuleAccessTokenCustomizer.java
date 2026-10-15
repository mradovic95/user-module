package com.comex.usermodule.authorizationserver;

import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.util.StringUtils;

import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.core.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shapes access tokens from {@code /oauth2/token} exactly like the ones {@link JwtAccessTokenIssuer} hands out on
 * login: {@code sub} is the user's email, {@code roles} the comma-joined authority list of the module user, plus
 * {@code email}/{@code name}. The audience is always every configured protected resource, whatever {@code resource}
 * parameter the client sent: one token works on every resource of the application.
 */
@Slf4j
@RequiredArgsConstructor
public class UserModuleAccessTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	public static final String ROLES_CLAIM = "roles";

	private final UserService userService;
	private final ProtectedResources protectedResources;

	@Override
	public void customize(JwtEncodingContext context) {
		if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			return;
		}
		// overrides both Spring's default (the client id) and the MCP resource-indicator customizer. A mutable list:
		// the claims are persisted with the authorization, and the JDBC store's Jackson type validator refuses
		// java.util.ImmutableCollections, so a List.of / toList() here breaks every later refresh
		context.getClaims().audience(new ArrayList<>(protectedResources.resourceUris()));
		Authentication principal = context.getPrincipal();
		String email = emailOf(principal);
		if (!StringUtils.hasText(email)) {
			log.warn("Access token for principal '{}' carries no email; leaving the default claims.",
				principal.getName());
			return;
		}
		String roles = userService.findByEmailOptional(email)
			.map(user -> String.join(",", user.getAuthorities()))
			.orElse("");
		context.getClaims()
			.subject(email)
			.claim(StandardClaimNames.EMAIL, email)
			.claim(ROLES_CLAIM, roles);
		String name = nameOf(principal);
		if (StringUtils.hasText(name)) {
			context.getClaims().claim(StandardClaimNames.NAME, name);
		}
	}

	private static String emailOf(Authentication principal) {
		if (principal.getPrincipal() instanceof OAuth2User oAuth2User) {
			return oAuth2User.getAttribute(StandardClaimNames.EMAIL);
		}
		return principal.getName() != null && principal.getName().contains("@") ? principal.getName() : null;
	}

	private static String nameOf(Authentication principal) {
		return principal.getPrincipal() instanceof OAuth2User oAuth2User
			? oAuth2User.getAttribute(StandardClaimNames.NAME)
			: null;
	}
}
