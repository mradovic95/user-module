package com.comex.usermodule.security;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_PASSWORD;
import static com.comex.usermodule.core.helper.UserTestInventory.loginUserDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.comex.usermodule.core.service.JwtService;

@ExtendWith(MockitoExtension.class)
class UserSpringAuthenticatorTest {

	private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@Captor
	private ArgumentCaptor<com.comex.usermodule.core.domain.User> userCaptor;

	private UserSpringAuthenticator sut;

	@BeforeEach
	void setUp() {
		sut = new UserSpringAuthenticator(authenticationManager, jwtService);
	}

	@Test
	void testAuthenticate() {
		// GIVEN
		User principal = new User(DEFAULT_EMAIL, "encoded",
			List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
		UsernamePasswordAuthenticationToken authenticated =
			UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authenticated);
		when(jwtService.generateToken(any())).thenReturn(TOKEN);

		// WHEN
		String result = sut.authenticate(loginUserDto());

		// THEN
		assertThat(result).isEqualTo(TOKEN);
		verify(authenticationManager).authenticate(argThat(auth ->
			DEFAULT_EMAIL.equals(auth.getPrincipal()) && DEFAULT_PASSWORD.equals(auth.getCredentials())));
		verify(jwtService).generateToken(userCaptor.capture());
		assertThat(userCaptor.getValue().getEmail()).isEqualTo(DEFAULT_EMAIL);
		assertThat(userCaptor.getValue().getAuthorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
	}
}
