package com.comex.usermodule.configuration;

import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.port.AccessTokenIssuer;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.security.UserSpringAuthenticator;

import lombok.extern.slf4j.Slf4j;

/**
 * Password authentication: BCrypt {@link PasswordEncoder}, the {@link AuthenticationProvider} backed by
 * {@link UserService}, and the {@link UserAuthenticator} behind {@code POST /user/login}. The tokens it hands out come
 * from the authorization server ({@link AccessTokenIssuer}), which also registers the module's default API chain.
 */
@Slf4j
@EnableMethodSecurity
@AutoConfiguration
public class SecurityConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public UserAuthenticator userAuthenticator(AuthenticationManager authenticationManager,
		AccessTokenIssuer accessTokenIssuer) {
		return new UserSpringAuthenticator(authenticationManager, accessTokenIssuer);
	}

	@ConditionalOnMissingBean
	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@ConditionalOnMissingBean
	@Bean
	public PasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@ConditionalOnMissingBean
	@Bean
	public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, UserService userService) {
		UserDetailsService userDetailsService = (email) -> {
			User user = userService.findByEmail(email);
			return new org.springframework.security.core.userdetails.User(
				user.getEmail(), user.getPassword(), user.getAuthorities().stream()
				.map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
		};
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return authenticationProvider;
	}
}
