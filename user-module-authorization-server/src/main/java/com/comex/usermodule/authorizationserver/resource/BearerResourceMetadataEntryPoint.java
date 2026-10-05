package com.comex.usermodule.authorizationserver.resource;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * The {@code 401} an MCP client needs to start sign-in: a {@code WWW-Authenticate: Bearer} challenge pointing at
 * the protected resource metadata (RFC 9728 section 5.1), plus the module's JSON error body.
 */
@RequiredArgsConstructor
public class BearerResourceMetadataEntryPoint implements AuthenticationEntryPoint {

	static final String BODY = "{\"error\": \"Unauthorized - Token is missing or invalid\"}";

	private final String metadataUrl;
	private final List<String> scopes;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		StringBuilder challenge = new StringBuilder("Bearer resource_metadata=\"").append(metadataUrl).append('"');
		if (scopes != null && !scopes.isEmpty()) {
			challenge.append(", scope=\"").append(String.join(" ", scopes)).append('"');
		}
		if (authException instanceof OAuth2AuthenticationException oauth2Exception) {
			OAuth2Error error = oauth2Exception.getError();
			challenge.append(", error=\"").append(error.getErrorCode()).append('"');
			if (StringUtils.hasText(error.getDescription())) {
				challenge.append(", error_description=\"").append(error.getDescription().replace("\"", "'"))
					.append('"');
			}
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge.toString());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(BODY);
	}
}
