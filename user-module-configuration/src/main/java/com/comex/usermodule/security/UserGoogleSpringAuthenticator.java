package com.comex.usermodule.security;

import java.util.UUID;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserGoogleSpringAuthenticator implements UserGoogleAuthenticator {

	private final UserService userService;
	private final JwtService jwtService;

	@Override
	public String authenticate(LoginUserOAuth2Dto loginUserOAuth2Dto) {
		User user = userService.findByEmailOptional(loginUserOAuth2Dto.email())
			.orElseGet(() -> userService.createUser(new CreateUserDto(loginUserOAuth2Dto.email(),
				UUID.randomUUID().toString(), loginUserOAuth2Dto.email())));

		return jwtService.generateToken(user);
	}
}
