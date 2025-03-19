package com.comex.usermodule.endpoint.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import com.comex.usermodule.core.service.JwtService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserSpringAuthenticator implements UserAuthenticator {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	@Override
	public String authenticate(LoginUserRequest loginUserRequest) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(loginUserRequest.email(), loginUserRequest.password()));
		User user = (User) authentication.getPrincipal();
		return jwtService.generateToken(user);
	}
}
