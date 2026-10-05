package com.comex.usermodule.security;

import static com.comex.usermodule.core.exception.UserExceptionKey.OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * Exchanges a Google identity for a module JWT, creating the user on first login. When {@code allowedDomains} is
 * non-empty, only emails whose domain is in the list may sign in; an empty list accepts every Google account.
 */
@Slf4j
public class UserGoogleSpringAuthenticator implements UserGoogleAuthenticator {

	private final UserService userService;
	private final JwtService jwtService;
	private final Set<String> allowedDomains;

	public UserGoogleSpringAuthenticator(UserService userService, JwtService jwtService,
		List<String> allowedDomains) {
		this.userService = userService;
		this.jwtService = jwtService;
		this.allowedDomains = allowedDomains == null ? Set.of() : allowedDomains.stream()
			.map(domain -> domain.trim().toLowerCase(Locale.ROOT))
			.filter(domain -> !domain.isEmpty())
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public String authenticate(LoginUserOAuth2Dto loginUserOAuth2Dto) {
		String email = loginUserOAuth2Dto.email();
		if (!isDomainAllowed(email)) {
			log.warn("Google login rejected for {}: email domain is not in the allowed list.", email);
			throw new UserException(OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED, Map.of("email", email),
				String.format("Email domain of %s is not allowed.", email));
		}

		User user = userService.findByEmailOptional(email)
			.orElseGet(() -> userService.createOAuth2User(loginUserOAuth2Dto));

		return jwtService.generateToken(user);
	}

	private boolean isDomainAllowed(String email) {
		if (allowedDomains.isEmpty()) {
			return true;
		}
		int at = email.lastIndexOf('@');
		if (at < 0 || at == email.length() - 1) {
			return false;
		}
		return allowedDomains.contains(email.substring(at + 1).toLowerCase(Locale.ROOT));
	}
}
