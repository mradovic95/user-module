package com.comex.usermodule.core.service;

import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.core.port.UserAuthenticator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserAuthenticationService {

	private final UserAuthenticator userAuthenticator;

	public String login(LoginUserDto loginUserDto) {
		log.info("Login user request: {}.", loginUserDto);
		return userAuthenticator.authenticate(loginUserDto);
	}
}
