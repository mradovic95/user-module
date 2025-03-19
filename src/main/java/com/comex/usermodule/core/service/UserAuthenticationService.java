package com.comex.usermodule.core.service;

import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.endpoint.model.LoginUserRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserAuthenticationService {

	private final UserAuthenticator userAuthenticator;

	public String login(LoginUserRequest loginUserRequest) {
		log.info("Login user request: {}.", loginUserRequest);
		return userAuthenticator.authenticate(loginUserRequest);
	}
}
