package com.comex.usermodule.authorizationserver;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.util.StringUtils;

import com.comex.usermodule.authorizationserver.resource.ProtectedResources;
import com.comex.usermodule.core.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shapes access tokens like the module's own JWTs: {@code sub} is the user's email and {@code roles} is the
 * comma-joined authority list of the module user, so resource servers can authenticate both token kinds the same
 * way. Also adds {@code email}/{@code name} and, when the client sent no {@code resource} parameter, binds the token
 * to every configured protected resource.
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
		List<String> resourceUris = protectedResources.resourceUris();
		if (!resourceUris.isEmpty()) {
			context.getClaims().claims(claims -> claims.putIfAbsent(JwtClaimNames.AUD, resourceUris));
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
