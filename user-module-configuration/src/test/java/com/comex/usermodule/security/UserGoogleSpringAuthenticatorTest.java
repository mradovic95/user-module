package com.comex.usermodule.security;

import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_EMAIL;
import static com.comex.usermodule.core.helper.UserTestInventory.DEFAULT_OAUTH2_NAME;
import static com.comex.usermodule.core.helper.UserTestInventory.loginUserOAuth2Dto;
import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserGoogleSpringAuthenticatorTest {

	private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";

	@Mock
	private UserService userService;

	@Mock
	private JwtService jwtService;

	private UserGoogleSpringAuthenticator sut;

	@BeforeEach
	void setUp() {
		// empty allowed-domains list: every Google account is accepted
		sut = new UserGoogleSpringAuthenticator(userService, jwtService, List.of());
	}

	@Test
	void testAuthenticateExistingUser() {
		// GIVEN
		LoginUserOAuth2Dto loginUserOAuth2Dto = loginUserOAuth2Dto();
		User existingUser = verifiedUser();

		when(userService.findByEmailOptional(DEFAULT_EMAIL)).thenReturn(Optional.of(existingUser));
		when(jwtService.generateToken(existingUser)).thenReturn(TOKEN);

		// WHEN
		String result = sut.authenticate(loginUserOAuth2Dto);

		// THEN
		assertThat(result).isEqualTo(TOKEN);
		verify(userService, never()).createOAuth2User(loginUserOAuth2Dto);
	}

	@Test
	void testAuthenticateNewUser() {
		// GIVEN
		LoginUserOAuth2Dto loginUserOAuth2Dto = loginUserOAuth2Dto();
		User newUser = verifiedUser();

		when(userService.findByEmailOptional(DEFAULT_EMAIL)).thenReturn(Optional.empty());
		when(userService.createOAuth2User(loginUserOAuth2Dto)).thenReturn(newUser);
		when(jwtService.generateToken(newUser)).thenReturn(TOKEN);

		// WHEN
		String result = sut.authenticate(loginUserOAuth2Dto);

		// THEN
		assertThat(result).isEqualTo(TOKEN);
		verify(userService).createOAuth2User(loginUserOAuth2Dto);
	}

	@ParameterizedTest
	@MethodSource("provideDomainPolicyScenarios")
	void testAuthenticateDomainPolicy(String email, List<String> allowedDomains, boolean allowed) {
		// GIVEN
		sut = new UserGoogleSpringAuthenticator(userService, jwtService, allowedDomains);
		LoginUserOAuth2Dto loginUserOAuth2Dto = loginUserOAuth2Dto(email, DEFAULT_OAUTH2_NAME);
		if (allowed) {
			User user = verifiedUser();
			when(userService.findByEmailOptional(email)).thenReturn(Optional.of(user));
			when(jwtService.generateToken(user)).thenReturn(TOKEN);
		}

		// WHEN / THEN
		if (allowed) {
			assertThat(sut.authenticate(loginUserOAuth2Dto)).isEqualTo(TOKEN);
		} else {
			assertThatThrownBy(() -> sut.authenticate(loginUserOAuth2Dto))
				.isInstanceOf(UserException.class)
				.extracting("errorKey")
				.isEqualTo(UserExceptionKey.OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED);
			verifyNoInteractions(userService, jwtService);
		}
	}

	private static Stream<Arguments> provideDomainPolicyScenarios() {
		List<String> domains = List.of("comex.com", "LeanPay.com");
		return Stream.of(
			Arguments.of("ana@comex.com", domains, true),
			Arguments.of("Ana@COMEX.COM", domains, true),
			Arguments.of("mihailo@leanpay.com", domains, true),
			Arguments.of("someone@gmail.com", domains, false),
			Arguments.of("attacker@comex.com.evil.net", domains, false),
			Arguments.of("attacker@evil.comex.com", domains, false),
			Arguments.of("no-at-sign", domains, false),
			Arguments.of("trailing@", domains, false),
			Arguments.of("someone@gmail.com", List.of(), true),
			Arguments.of("someone@gmail.com", List.of(" "), true)
		);
	}
}
