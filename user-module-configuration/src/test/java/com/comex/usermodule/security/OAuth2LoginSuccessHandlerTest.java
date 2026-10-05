package com.comex.usermodule.security;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_OAUTH2_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

	private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";
	private static final String REDIRECT_URL = "https://app.example.com/oauth/callback";

	@Mock
	private UserGoogleAuthenticator userGoogleAuthenticator;

	private OAuth2LoginSuccessHandler sut;

	@Test
	void testOnAuthenticationSuccessWritesJsonWhenNoRedirectUrl() throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, null);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(userGoogleAuthenticator.authenticate(new LoginUserOAuth2Dto(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME)))
			.thenReturn(TOKEN);

		// WHEN
		sut.onAuthenticationSuccess(request, response, authentication(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME, true));

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).isEqualTo("{\"token\": \"" + TOKEN + "\"}");
	}

	@Test
	void testOnAuthenticationSuccessRedirectsWhenRedirectUrlConfigured() throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, REDIRECT_URL);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(userGoogleAuthenticator.authenticate(new LoginUserOAuth2Dto(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME)))
			.thenReturn(TOKEN);

		// WHEN
		// no email_verified attribute at all: treated as verified
		sut.onAuthenticationSuccess(request, response, authentication(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME, null));

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
		assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL + "?token=" + TOKEN);
		assertThat(response.getContentAsString()).isEmpty();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = REDIRECT_URL)
	void testOnAuthenticationSuccessResumesSavedRequest(String redirectUrl) throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, redirectUrl);
		MockHttpServletRequest authorizeRequest = new MockHttpServletRequest("GET", "/oauth2/authorize");
		authorizeRequest.setQueryString("client_id=claude&response_type=code");
		MockHttpServletResponse response = new MockHttpServletResponse();
		new HttpSessionRequestCache().saveRequest(authorizeRequest, response);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(authorizeRequest.getSession());
		when(userGoogleAuthenticator.authenticate(new LoginUserOAuth2Dto(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME)))
			.thenReturn(TOKEN);

		// WHEN
		sut.onAuthenticationSuccess(request, response, authentication(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME, true));

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
		assertThat(response.getRedirectedUrl())
			.startsWith("http://localhost/oauth2/authorize?client_id=claude&response_type=code")
			.doesNotContain(TOKEN);
		assertThat(response.getContentAsString()).isEmpty();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = REDIRECT_URL)
	void testOnAuthenticationSuccessWithoutEmail(String redirectUrl) throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, redirectUrl);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		// WHEN
		sut.onAuthenticationSuccess(request, response, authentication(null, DEFAULT_OAUTH2_NAME, true));

		// THEN
		assertRejected(response, redirectUrl, HttpServletResponse.SC_UNAUTHORIZED, "email_missing",
			"did not return an email");
		verifyNoInteractions(userGoogleAuthenticator);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = REDIRECT_URL)
	void testOnAuthenticationSuccessWithUnverifiedEmail(String redirectUrl) throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, redirectUrl);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		// WHEN
		sut.onAuthenticationSuccess(request, response, authentication(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME, false));

		// THEN
		assertRejected(response, redirectUrl, HttpServletResponse.SC_UNAUTHORIZED, "email_not_verified",
			"not verified");
		verifyNoInteractions(userGoogleAuthenticator);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = REDIRECT_URL)
	void testOnAuthenticationSuccessWhenDomainNotAllowed(String redirectUrl) throws Exception {
		// GIVEN
		sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator, redirectUrl);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(userGoogleAuthenticator.authenticate(new LoginUserOAuth2Dto(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME)))
			.thenThrow(new UserException(UserExceptionKey.OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED, "not allowed"));

		// WHEN
		sut.onAuthenticationSuccess(request, response, authentication(DEFAULT_EMAIL, DEFAULT_OAUTH2_NAME, true));

		// THEN
		assertRejected(response, redirectUrl, HttpServletResponse.SC_FORBIDDEN, "domain_not_allowed",
			"domain is not allowed");
	}

	private static void assertRejected(MockHttpServletResponse response, String redirectUrl, int expectedStatus,
		String expectedErrorCode, String expectedMessageFragment) throws Exception {
		if (redirectUrl != null) {
			assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
			assertThat(response.getRedirectedUrl()).isEqualTo(redirectUrl + "?error=" + expectedErrorCode);
			assertThat(response.getContentAsString()).isEmpty();
		} else {
			assertThat(response.getStatus()).isEqualTo(expectedStatus);
			assertThat(response.getRedirectedUrl()).isNull();
			assertThat(response.getContentType()).startsWith("application/json");
			assertThat(response.getContentAsString()).contains("\"error\"").contains(expectedMessageFragment);
		}
	}

	private static OAuth2AuthenticationToken authentication(String email, String name, Boolean emailVerified) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", "google-subject");
		attributes.put("name", name);
		if (email != null) {
			attributes.put("email", email);
		}
		if (emailVerified != null) {
			attributes.put("email_verified", emailVerified);
		}
		OAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
			attributes, "sub");
		return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
	}
}
