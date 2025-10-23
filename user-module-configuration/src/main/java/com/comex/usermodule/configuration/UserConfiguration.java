package com.comex.usermodule.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.comex.usermodule.adapter.PasswordEncoderAdapter;
import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.PasswordEncoder;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.core.service.UserAuthenticationService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.core.service.UserVerificationService;
import com.comex.usermodule.infrastructure.messaging.EventPublisherMock;

@AutoConfiguration
public class UserConfiguration {

	@Autowired
	private UserProperties userProperties;

	@Bean(name = "passwordEncoderAdapter")
	public PasswordEncoder passwordEncoder(
		org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder) {

		return new PasswordEncoderAdapter(springPasswordEncoder);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserMapper userMapper(PasswordEncoder passwordEncoderAdapter) {
		return new UserMapper(passwordEncoderAdapter);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserAuthenticationService userAuthenticationService(UserAuthenticator userAuthenticator) {
		return new UserAuthenticationService(userAuthenticator);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserVerificationService userVerificationService(UserRepository userRepository, EventPublisher eventPublisher,
		UserMapper userMapper) {
		return new UserVerificationService(userRepository, eventPublisher, userMapper);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserService userService(UserRepository userRepository, EventPublisher eventPublisher,
		UserMapper userMapper) {

		return new UserService(userProperties.isVerificationRequired(), userRepository, eventPublisher, userMapper);
	}

	@ConditionalOnMissingBean
	@Bean
	public EventPublisher eventPublisherMock() {
		return new EventPublisherMock();
	}
}
