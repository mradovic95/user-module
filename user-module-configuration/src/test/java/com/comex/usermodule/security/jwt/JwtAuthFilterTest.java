package com.comex.usermodule.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import com.comex.usermodule.core.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

	private static final String TOKEN = "jwt-token";

	@Mock
	private JwtService jwtService;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;
	private JwtAuthFilter sut;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		request = new MockHttpServletRequest("GET", "/user");
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		sut = new JwtAuthFilter(jwtService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testDoFilterInternalWithValidToken() throws Exception {
		// GIVEN
		Claims claims = Jwts.claims().subject("john@example.com").add("roles", "ROLE_USER,ROLE_ADMIN").build();
		request.addHeader("Authorization", "Bearer " + TOKEN);
		when(jwtService.extractAllClaims(TOKEN)).thenReturn(claims);

		// WHEN
		sut.doFilterInternal(request, response, filterChain);

		// THEN
		assertThat(filterChain.getRequest()).isSameAs(request);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("john@example.com");
		assertThat(authentication.getAuthorities()).extracting("authority")
			.containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"Basic dXNlcjpwYXNz", "Token abc"})
	void testDoFilterInternalWithoutBearerToken(String authorizationHeader) throws Exception {
		// GIVEN
		if (authorizationHeader != null) {
			request.addHeader("Authorization", authorizationHeader);
		}

		// WHEN
		sut.doFilterInternal(request, response, filterChain);

		// THEN
		verify(jwtService, never()).extractAllClaims(anyString());
		assertThat(filterChain.getRequest()).isSameAs(request);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternalWithInvalidToken() throws Exception {
		// GIVEN
		request.addHeader("Authorization", "Bearer " + TOKEN);
		when(jwtService.extractAllClaims(TOKEN))
			.thenThrow(new UserException(UserExceptionKey.JWT_TOKEN_INVALID, "invalid"));

		// WHEN
		sut.doFilterInternal(request, response, filterChain);

		// THEN
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		assertThat(response.getContentAsString()).contains("Invalid or expired JWT token");
		assertThat(filterChain.getRequest()).isNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternalDoesNotOverwriteExistingAuthentication() throws Exception {
		// GIVEN
		Claims claims = Jwts.claims().subject("john@example.com").add("roles", "ROLE_USER").build();
		request.addHeader("Authorization", "Bearer " + TOKEN);
		when(jwtService.extractAllClaims(TOKEN)).thenReturn(claims);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("existing@example.com", null, Collections.emptyList()));

		// WHEN
		sut.doFilterInternal(request, response, filterChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing@example.com");
		assertThat(filterChain.getRequest()).isSameAs(request);
	}
}
