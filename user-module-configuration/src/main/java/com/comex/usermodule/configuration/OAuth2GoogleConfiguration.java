package com.comex.usermodule.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.security.OAuth2LoginSuccessHandler;
import com.comex.usermodule.security.UserGoogleSpringAuthenticator;

/**
 * Google OAuth2 login.
 * <p>
 * The authenticator and success handler beans are always registered. The login itself runs in its own
 * session-based {@link SecurityFilterChain} covering only the OAuth2 client paths, registered when
 * {@code spring.security.oauth2.client.registration.*} properties are present. Keeping it separate from the API
 * chain means it keeps working when an application replaces the API chain with its own, and that the same login
 * serves both the REST token hand-off and the authorization server's {@code /oauth2/authorize} flow.
 */
@AutoConfiguration
public class OAuth2GoogleConfiguration {

	/** Bean name of the Google login chain. */
	public static final String GOOGLE_LOGIN_CHAIN_BEAN_NAME = "userModuleGoogleLoginSecurityFilterChain";

	/** Runs before application chains and after the authorization server chain. */
	public static final int GOOGLE_LOGIN_CHAIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

	/** Paths owned by the Google login chain. */
	public static final String[] GOOGLE_LOGIN_PATHS = { "/oauth2/authorization/**", "/login/oauth2/**" };

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

	/**
	 * Session-based chain for the OAuth2 client endpoints only. A session is needed to keep the OAuth2 state between
	 * the redirect to Google and the callback, and to resume a request saved by the authorization server chain.
	 */
	@ConditionalOnOAuth2ClientRegistration
	@Bean(GOOGLE_LOGIN_CHAIN_BEAN_NAME)
	@Order(GOOGLE_LOGIN_CHAIN_ORDER)
	public SecurityFilterChain userModuleGoogleLoginSecurityFilterChain(HttpSecurity http,
		OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) throws Exception {
		return http
			.securityMatcher(GOOGLE_LOGIN_PATHS)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
			.build();
	}
}
