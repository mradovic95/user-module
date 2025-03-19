package com.comex.usermodule.core.service;


import static com.comex.usermodule.core.domain.UserStatus.VERIFIED;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserVerificationService {

	private final UserRepository userRepository;
	private final EventPublisher eventPublisher;
	private final UserMapper userMapper;

	public void verify(String verificationCode) {
		log.info("Verifying user with verification code: {}.", verificationCode);
		User user = userRepository.findByVerificationCode(verificationCode);
		user.setStatus(VERIFIED);
		userRepository.save(user);
		// publish event
		eventPublisher.publish(userMapper.toUserVerifiedEvent(user));
	}
}
