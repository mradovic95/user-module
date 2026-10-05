package com.comex.usermodule.authorizationserver.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import jakarta.servlet.http.HttpServletResponse;

class BearerResourceMetadataEntryPointTest {

	private static final String METADATA_URL = "https://api.example.com/.well-known/oauth-protected-resource/mcp";

	private BearerResourceMetadataEntryPoint sut;

	@Test
	void testCommenceWithoutTokenPointsAtResourceMetadata() throws Exception {
		// GIVEN
		sut = new BearerResourceMetadataEntryPoint(METADATA_URL, List.of());
		MockHttpServletResponse response = new MockHttpServletResponse();

		// WHEN
		sut.commence(new MockHttpServletRequest(), response, new InsufficientAuthenticationException("none"));

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		assertThat(response.getHeader("WWW-Authenticate"))
			.isEqualTo("Bearer resource_metadata=\"" + METADATA_URL + "\"");
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).isEqualTo(BearerResourceMetadataEntryPoint.BODY);
	}

	@Test
	void testCommenceWithScopesAndInvalidTokenAddsChallengeParameters() throws Exception {
		// GIVEN
		sut = new BearerResourceMetadataEntryPoint(METADATA_URL, List.of("tools", "datasets"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
			new OAuth2Error("invalid_token", "Jwt expired at \"2026-01-01\"", null));

		// WHEN
		sut.commence(new MockHttpServletRequest(), response, exception);

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer resource_metadata=\"" + METADATA_URL
			+ "\", scope=\"tools datasets\", error=\"invalid_token\", error_description=\"Jwt expired at '2026-01-01'\"");
	}
}
