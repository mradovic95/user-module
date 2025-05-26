package com.comex.usermodule.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.endpoint.security.OAuth2LoginSuccessHandler;
import com.comex.usermodule.endpoint.security.UserGoogleSpringAuthenticator;

@AutoConfiguration
public class OAuth2GoogleConfiguration {

	@Bean
	public UserGoogleAuthenticator userGoogleAuthenticator(UserService userService, JwtService jwtService) {
		return new UserGoogleSpringAuthenticator(userService, jwtService);
	}

	@Bean
	public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(UserGoogleAuthenticator userGoogleAuthenticator) {
		return new OAuth2LoginSuccessHandler(userGoogleAuthenticator);
	}
}
