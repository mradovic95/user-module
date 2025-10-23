package com.comex.usermodule.starter.postgre.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.jpa.RolePostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.jpa.UserPostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.mapper.UserEntityMapper;
import com.comex.usermodule.infrastructure.persistence.postgre.repository.UserPostgreRepository;

import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@ConditionalOnProperty(name = "user.persistence.type", havingValue = "postgresql", matchIfMissing = true)
@EntityScan(basePackages = "com.comex.usermodule.infrastructure.persistence.postgre.entity")
@EnableJpaRepositories(basePackages = "com.comex.usermodule.infrastructure.persistence.postgre.jpa")
@Slf4j
public class UserPostgreRepositoryConfiguration {

	@ConditionalOnMissingBean(UserRepository.class)
	@Bean
	public UserRepository userRepository(UserPostgreJpaRepository jpaRepository,
		RolePostgreJpaRepository roleJpaRepository, UserEntityMapper userEntityMapper) {

		return new UserPostgreRepository(jpaRepository, roleJpaRepository, userEntityMapper);
	}

	@ConditionalOnMissingBean
	@Bean
	public UserEntityMapper userEntityMapper() {
		return new UserEntityMapper();
	}
}
