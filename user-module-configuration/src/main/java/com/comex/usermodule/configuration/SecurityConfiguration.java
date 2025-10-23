package com.comex.usermodule.configuration;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.security.UserSpringAuthenticator;
import com.comex.usermodule.security.jwt.JwtAuthFilter;

@AutoConfiguration
public class SecurityConfiguration {

	@Autowired
	private UserProperties userProperties;

	@ConditionalOnMissingBean
	@Bean
	public UserAuthenticator userAuthenticator(AuthenticationManager authenticationManager, JwtService jwtService) {
		return new UserSpringAuthenticator(authenticationManager, jwtService);
	}

	@ConditionalOnMissingBean
	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@ConditionalOnMissingBean
	@Bean
	public JwtService jwtService() {
		return new JwtService(userProperties.getJwt().getJwtSecretKey(), userProperties.getJwt().getJwtExpiration());
	}

	@ConditionalOnMissingBean
	@Bean
	public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
		return new JwtAuthFilter(jwtService);
	}

	@ConditionalOnMissingBean
	@Bean
	public PasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@ConditionalOnMissingBean
	@Bean
	public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, UserService userService) {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService((email) -> {
			User user = userService.findByEmail(email);
			return new org.springframework.security.core.userdetails.User(
				user.getEmail(), user.getPassword(), user.getAuthorities().stream()
				.map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
		});
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return authenticationProvider;
	}
}
