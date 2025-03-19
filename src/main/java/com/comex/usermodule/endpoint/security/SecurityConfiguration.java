package com.comex.usermodule.endpoint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.endpoint.security.jwt.JwtAuthFilter;
import com.comex.usermodule.core.service.JwtService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	private final String jwtSecretKey = "sadasdasdasdaksdlaksdlsadaslfjaslasdasdasdsdsadsadaksdasdlasdalsdkslakdlsakdlaskdslakdlakdlakd";
	private final Long jwtExpiration = 3600L * 1000L;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
		AuthenticationProvider authenticationProvider) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/user/**").permitAll()
//				.requestMatchers("/notification/**").hasRole("ADMIN")
//				.requestMatchers("/influencer/**").hasAnyRole("USER", "ADMIN")
					.requestMatchers("/campaign/**").hasAnyRole("USER", "ADMIN")
					.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
					.anyRequest().authenticated()
			)
			.sessionManagement(sess -> sess
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authenticationProvider(authenticationProvider)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(((request, response, authException) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\": \"Unauthorized - Token is missing or invalid\"}");
				}))
				.accessDeniedHandler(((request, response, authException) -> {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\": \"Access denied\"}");
				}))
			);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public JwtService jwtService() {
		return new JwtService(jwtSecretKey, jwtExpiration);
	}

	@Bean
	public JwtAuthFilter jwtAuthFilter(JwtService jwtService) throws Exception {
		return new JwtAuthFilter(jwtService);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, UserService userService) {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userService::findByEmail);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return authenticationProvider;
	}
}
