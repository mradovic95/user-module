package com.comex.usermodule.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.core.service.UserAuthenticationService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.core.service.UserVerificationService;
import com.comex.usermodule.infrastructure.persistence.jpa.RolePostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.jpa.UserPostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.mapper.UserEntityMapper;
import com.comex.usermodule.infrastructure.messaging.EventPublisherMock;
import com.comex.usermodule.infrastructure.persistence.repository.UserPostgreRepository;

import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@EntityScan(basePackages = "com.comex.usermodule.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.comex.usermodule.infrastructure.persistence.jpa")
@Slf4j
public class UserConfiguration {

	@Autowired
	private UserProperties userProperties;

	@ConditionalOnMissingBean
	@Bean
	public UserMapper userMapper(PasswordEncoder passwordEncoder) {
		return new UserMapper(passwordEncoder);
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
	public UserPostgreRepository userPostgreRepository(UserPostgreJpaRepository jpaRepository,
		RolePostgreJpaRepository roleJpaRepository, UserEntityMapper userEntityMapper) {

		return new UserPostgreRepository(jpaRepository, roleJpaRepository, userEntityMapper);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserEntityMapper userEntityMapper() {
		return new UserEntityMapper();
	}

	@ConditionalOnMissingBean
	@Bean
	public EventPublisher eventPublisherMock() {
		return new EventPublisherMock();
	}
}
