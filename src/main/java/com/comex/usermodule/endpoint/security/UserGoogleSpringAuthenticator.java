package com.comex.usermodule.endpoint.security;

import java.util.UUID;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserOAuth2Request;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserGoogleSpringAuthenticator implements UserGoogleAuthenticator {

	private final UserService userService;
	private final JwtService jwtService;

	@Override
	public String authenticate(LoginUserOAuth2Request loginUserOAuth2Request) {
		User user = userService.findByEmailOptional(loginUserOAuth2Request.email())
			.orElseGet(() -> userService.createUser(new CreateUserDto(loginUserOAuth2Request.email(),
				UUID.randomUUID().toString(), loginUserOAuth2Request.email())));

		return jwtService.generateToken(user);
	}
}
