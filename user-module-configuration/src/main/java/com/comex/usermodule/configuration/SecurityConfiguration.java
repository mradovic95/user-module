package com.comex.usermodule.configuration;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.security.UserSpringAuthenticator;
import com.comex.usermodule.security.jwt.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableMethodSecurity
@AutoConfiguration
public class SecurityConfiguration {

	/** Bean name of the module's default stateless JWT API chain. */
	public static final String API_CHAIN_BEAN_NAME = "userModuleSecurityFilterChain";

	@Autowired
	private UserProperties userProperties;

	/**
	 * Default stateless JWT API chain. Consumers replace it by defining their own {@link SecurityFilterChain} bean;
	 * the module's other chains (Google login, authorization server) are kept in that case.
	 * <p>
	 * Google OAuth2 login does not live here: {@link OAuth2GoogleConfiguration} registers a separate, session-based
	 * chain for {@code /oauth2/authorization/**} and {@code /login/oauth2/**} when Google client properties are set.
	 */
	@ConditionalOnMissingApplicationSecurityFilterChain
	@Bean(API_CHAIN_BEAN_NAME)
	public SecurityFilterChain userModuleSecurityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
		AuthenticationProvider authenticationProvider) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/user", "/user/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/user/verify").permitAll()
				.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated()
			)
			.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authenticationProvider(authenticationProvider)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, authException) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\": \"Unauthorized - Token is missing or invalid\"}");
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\": \"Access denied\"}");
				})
			)
			.build();
	}

	/**
	 * {@link JwtAuthFilter} is a {@code Filter} bean, so Spring Boot would also register it as a plain servlet
	 * filter outside the security chain. Disable that registration; the security chain adds it explicitly.
	 */
	@Bean
	public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
		FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
		registration.setEnabled(false);
		return registration;
	}

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
