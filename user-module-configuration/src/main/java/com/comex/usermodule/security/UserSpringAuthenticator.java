package com.comex.usermodule.security;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.service.JwtService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserSpringAuthenticator implements UserAuthenticator {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	@Override
	public String authenticate(LoginUserDto loginUserDto) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(loginUserDto.email(), loginUserDto.password()));
		User user = (User) authentication.getPrincipal();
		return jwtService.generateToken(com.comex.usermodule.core.domain.User
			.builder()
			.email(user.getUsername())
			.roles(user.getAuthorities().stream()
				.map(a -> new Role(a.getAuthority(), Collections.emptySet()))
				.collect(Collectors.toSet()))
			.build());
	}
}
