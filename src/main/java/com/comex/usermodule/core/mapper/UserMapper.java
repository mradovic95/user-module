package com.comex.usermodule.core.mapper;

import static com.comex.usermodule.core.domain.UserStatus.CREATED;
import static com.comex.usermodule.core.domain.UserStatus.VERIFIED;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.event.UserCreatedEvent;
import com.comex.usermodule.core.event.UserVerifiedEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserMapper {

	private final PasswordEncoder passwordEncoder;

	public User toUser(CreateUserDto registerUserDto, boolean verificationRequired) {
		return User.builder()
			.username(registerUserDto.username())
			.password(passwordEncoder.encode(registerUserDto.password()))
			.email(registerUserDto.email())
			.createdAt(Instant.now())
			.roles(Collections.singleton(new Role("ROLE_USER")))
			.status(verificationRequired ? CREATED : VERIFIED)
			.verificationCode(UUID.randomUUID().toString())
			.build();
	}

	public UserCreatedEvent toUserCreatedEvent(User user) {

		return UserCreatedEvent.builder()
			.id(user.getId())
			.timestamp(Instant.now())
			.username(user.getUsername())
			.email(user.getUsername())
			.build();
	}

	public UserVerifiedEvent toUserVerifiedEvent(User user) {

		return UserVerifiedEvent.builder()
			.id(user.getId())
			.timestamp(Instant.now())
			.verificationCode(user.getVerificationCode())
			.build();
	}
}
