package com.comex.usermodule.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.security.OAuth2LoginSuccessHandler;
import com.comex.usermodule.security.UserGoogleSpringAuthenticator;

/**
 * Beans for the Google OAuth2 login flow. They are always registered; the flow itself is only switched on in the
 * {@link SecurityConfiguration} filter chain when Spring Boot has created a {@code ClientRegistrationRepository},
 * i.e. when {@code spring.security.oauth2.client.registration.google.client-id/client-secret} are set.
 */
@AutoConfiguration
public class OAuth2GoogleConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public UserGoogleAuthenticator userGoogleAuthenticator(UserService userService, JwtService jwtService,
		UserProperties userProperties) {
		return new UserGoogleSpringAuthenticator(userService, jwtService,
			userProperties.getOauth2().getAllowedDomains());
	}

	@ConditionalOnMissingBean
	@Bean
	public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(UserGoogleAuthenticator userGoogleAuthenticator,
		UserProperties userProperties) {
		return new OAuth2LoginSuccessHandler(userGoogleAuthenticator,
			userProperties.getOauth2().getSuccessRedirectUrl());
	}
}
