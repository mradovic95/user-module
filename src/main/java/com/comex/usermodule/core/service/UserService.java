package com.comex.usermodule.core.service;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
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

	public void createUser(CreateUserDto createUserDto) {
		log.info("Saving user: {}.", createUserDto);
		User user = userRepository.save(userMapper.toUser(createUserDto, verificationRequired));
		// publish event
		eventPublisher.publish(userMapper.toUserCreatedEvent(user));
	}

	public User findByEmail(String email) {
		return userRepository.findByEmail(email);
	}
}
