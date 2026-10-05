package com.comex.usermodule.authorizationserver.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

class ProtectedResourceMetadataFilterTest {

	private static final String ISSUER = "https://api.example.com";

	private ProtectedResourceMetadataFilter sut;

	@BeforeEach
	void setUp() {
		sut = new ProtectedResourceMetadataFilter(ProtectedResources.of(ISSUER, List.of("/mcp"), List.of("tools")));
	}

	@Test
	void testServesMetadataForConfiguredResource() throws Exception {
		// GIVEN
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/oauth-protected-resource/mcp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		// WHEN
		sut.doFilter(request, response, chain);

		// THEN
		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).isEqualTo("{\"resource\":\"" + ISSUER + "/mcp\","
			+ "\"authorization_servers\":[\"" + ISSUER + "\"],\"bearer_methods_supported\":[\"header\"],"
			+ "\"scopes_supported\":[\"tools\"]}");
	}

	@Test
	void testRespondsNotFoundForUnknownResource() throws Exception {
		// GIVEN
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/oauth-protected-resource/other");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		// WHEN
		sut.doFilter(request, response, chain);

		// THEN
		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	void testPassesOtherRequestsThrough() throws Exception {
		// GIVEN
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/.well-known/oauth-protected-resource/mcp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		// WHEN
		sut.doFilter(request, response, chain);

		// THEN
		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentAsString()).isEmpty();
	}
}
