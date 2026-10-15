package com.comex.usermodule.authorizationserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads a {@code grant_type=password} request on the token endpoint: {@code username}, {@code password} and an
 * optional {@code scope}. The client was already authenticated by the client authentication filter.
 */
public final class PasswordGrantAuthenticationConverter implements AuthenticationConverter {

	/** Spring Security 7 dropped these with the password grant. */
	static final String USERNAME = "username";
	static final String PASSWORD = "password";

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2";
	private static final List<String> GRANT_PARAMETERS = List.of(OAuth2ParameterNames.GRANT_TYPE,
		USERNAME, PASSWORD, OAuth2ParameterNames.SCOPE,
		OAuth2ParameterNames.CLIENT_ID);

	@Override
	public Authentication convert(HttpServletRequest request) {
		if (!PasswordGrantAuthenticationToken.PASSWORD.getValue()
			.equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))) {
			return null;
		}
		String username = singleParameter(request, USERNAME);
		String password = singleParameter(request, PASSWORD);
		String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
		Set<String> scopes = StringUtils.hasText(scope)
			? Arrays.stream(scope.split(" ")).filter(StringUtils::hasText).collect(Collectors.toSet())
			: Set.of();
		Map<String, Object> additionalParameters = new HashMap<>();
		request.getParameterMap().forEach((key, values) -> {
			if (!GRANT_PARAMETERS.contains(key)) {
				additionalParameters.put(key, values.length == 1 ? values[0] : values);
			}
		});
		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
		return new PasswordGrantAuthenticationToken(username, password, scopes, clientPrincipal, additionalParameters);
	}

	private static String singleParameter(HttpServletRequest request, String name) {
		String[] values = request.getParameterValues(name);
		if (values == null || values.length != 1 || !StringUtils.hasText(values[0])) {
			throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
				"OAuth 2.0 Parameter: " + name, ERROR_URI));
		}
		return values[0];
	}
}
