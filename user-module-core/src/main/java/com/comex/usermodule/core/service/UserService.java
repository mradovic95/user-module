package com.comex.usermodule.core.service;

import java.util.Optional;
import java.util.UUID;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserService {

	private final Boolean verificationRequired;
	private final UserRepository userRepository;
	private final EventPublisher eventPublisher;
	private final UserMapper userMapper;

	public User createUser(CreateUserDto createUserDto) {
		log.info("Saving user: {}.", createUserDto);
		User user = userRepository.save(userMapper.toUser(createUserDto, verificationRequired));
		// publish event
		eventPublisher.publish(userMapper.toUserCreatedEvent(user));
		return user;
	}

	/**
	 * Creates a user from an external identity provider (e.g. Google). The provider has already verified the
	 * email, so the user is always stored as VERIFIED regardless of the verification-required setting.
	 */
	public User createOAuth2User(LoginUserOAuth2Dto loginUserOAuth2Dto) {
		log.info("Saving OAuth2 user: {}.", loginUserOAuth2Dto.email());
		CreateUserDto createUserDto = new CreateUserDto(loginUserOAuth2Dto.email(), UUID.randomUUID().toString(),
			loginUserOAuth2Dto.email());
		User user = userRepository.save(userMapper.toUser(createUserDto, false));
		eventPublisher.publish(userMapper.toUserCreatedEvent(user));
		return user;
	}

	public User findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public Optional<User> findByEmailOptional(String email) {
		return userRepository.findByEmailOptional(email);
	}
}
