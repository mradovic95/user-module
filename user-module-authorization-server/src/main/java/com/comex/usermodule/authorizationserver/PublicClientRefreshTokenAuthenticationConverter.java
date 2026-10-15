package com.comex.usermodule.authorizationserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Identifies a public client on a {@code refresh_token} or {@code password} token request: {@code client_id} only, no
 * secret, no {@code Authorization} header. Spring's {@code PublicClientAuthenticationConverter} handles only the PKCE
 * code exchange, so without this converter a public client (every MCP client, the first-party client) could never
 * refresh its tokens or use the password grant.
 */
public final class PublicClientRefreshTokenAuthenticationConverter implements AuthenticationConverter {

	@Override
	public Authentication convert(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return null;
		}
		MultiValueMap<String, String> parameters = formParameters(request);
		if (!PublicClientRefreshTokenAuthenticationProvider.isPublicClientGrant(
			parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE))) {
			return null;
		}
		boolean confidentialClient = StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION))
			|| parameters.containsKey(OAuth2ParameterNames.CLIENT_SECRET)
			|| parameters.containsKey(OAuth2ParameterNames.CLIENT_ASSERTION);
		if (confidentialClient) {
			return null;
		}
		String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
		List<String> clientIdParams = parameters.get(OAuth2ParameterNames.CLIENT_ID);
		if (!StringUtils.hasText(clientId) || clientIdParams == null || clientIdParams.size() != 1) {
			return null;
		}
		parameters.remove(OAuth2ParameterNames.CLIENT_ID);
		Map<String, Object> additionalParameters = new HashMap<>();
		parameters.forEach((key, value) -> additionalParameters.put(key,
			value.size() == 1 ? value.get(0) : value.toArray(new String[0])));
		return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null,
			additionalParameters);
	}

	/** Body parameters only, as Spring's token endpoint converters read them (query string excluded). */
	static MultiValueMap<String, String> formParameters(HttpServletRequest request) {
		Set<String> queryKeys = queryParameterNames(request.getQueryString());
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		request.getParameterMap().forEach((key, values) -> {
			if (!queryKeys.contains(key) && values.length > 0) {
				for (String value : values) {
					parameters.add(key, value);
				}
			}
		});
		return parameters;
	}

	/** Exact names, never a substring match: {@code xclient_id=} in the query must not hide a body {@code client_id}. */
	private static Set<String> queryParameterNames(String queryString) {
		if (!StringUtils.hasText(queryString)) {
			return Set.of();
		}
		return Arrays.stream(queryString.split("&"))
			.map(pair -> pair.split("=", 2)[0])
			.map(name -> URLDecoder.decode(name, StandardCharsets.UTF_8))
			.collect(Collectors.toSet());
	}
}
