package com.comex.usermodule.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

class PublicClientRefreshTokenAuthenticationConverterTest {

	private final PublicClientRefreshTokenAuthenticationConverter sut = new PublicClientRefreshTokenAuthenticationConverter();

	@Test
	void testConvertPasswordGrantWithClientIdOnly() {
		// GIVEN
		MockHttpServletRequest request = tokenRequest("password");

		// WHEN
		OAuth2ClientAuthenticationToken result = (OAuth2ClientAuthenticationToken) sut.convert(request);

		// THEN
		assertThat(result.getPrincipal()).isEqualTo("frontend");
		assertThat(result.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
	}

	@Test
	void testConvertIgnoresOtherGrantTypesAndConfidentialClients() {
		// GIVEN
		MockHttpServletRequest authorizationCode = tokenRequest("authorization_code");
		MockHttpServletRequest noGrantType = tokenRequest(null);
		MockHttpServletRequest confidential = tokenRequest("refresh_token");
		confidential.addParameter("client_secret", "secret");

		// WHEN / THEN
		assertThat(sut.convert(authorizationCode)).isNull();
		assertThat(sut.convert(noGrantType)).isNull();
		assertThat(sut.convert(confidential)).isNull();
	}

	@Test
	void testConvertMatchesQueryParameterNamesExactly() {
		// GIVEN a query parameter whose name merely ends in client_id
		MockHttpServletRequest request = tokenRequest("refresh_token");
		request.setQueryString("xclient_id=other");
		request.addParameter("xclient_id", "other");

		// WHEN
		OAuth2ClientAuthenticationToken result = (OAuth2ClientAuthenticationToken) sut.convert(request);

		// THEN the body client_id is still read
		assertThat(result).isNotNull();
		assertThat(result.getPrincipal()).isEqualTo("frontend");
	}

	@Test
	void testConvertIgnoresClientIdFromTheQueryString() {
		// GIVEN client_id only in the query string
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
		request.addParameter("grant_type", "refresh_token");
		request.setQueryString("client_id=frontend");
		request.addParameter("client_id", "frontend");

		// WHEN / THEN
		assertThat(sut.convert(request)).isNull();
	}

	private static MockHttpServletRequest tokenRequest(String grantType) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
		if (grantType != null) {
			request.addParameter("grant_type", grantType);
		}
		request.addParameter("client_id", "frontend");
		return request;
	}
}
