package com.comex.usermodule.infrastructure.repository;

import static com.comex.usermodule.core.exception.UserExceptionKey.NOT_FOUND;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.entity.RoleEntity;
import com.comex.usermodule.infrastructure.entity.UserEntity;
import com.comex.usermodule.infrastructure.jpa.RolePostgreJpaRepository;
import com.comex.usermodule.infrastructure.jpa.UserPostgreJpaRepository;
import com.comex.usermodule.infrastructure.mapper.UserEntityMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserPostgreRepository implements UserRepository {

	private final UserPostgreJpaRepository jpaRepository;
	private final RolePostgreJpaRepository roleJpaRepository;
	private final UserEntityMapper userEntityMapper;

	@Override
	public User save(User user) {
		log.debug("Saving user {}.", user);
		// find role
		Set<RoleEntity> roles = roleJpaRepository.findAllByNameIn(user.getRoles()
			.stream()
			.map(Role::getName)
			.collect(Collectors.toSet()));
		// map and save user
		UserEntity userEntity = jpaRepository.save(userEntityMapper.toUserEntity(user, roles));
		return userEntityMapper.toUser(userEntity);
	}

	@Override
	public User findByEmail(String email) {
		log.debug("Finding verified user by email: {}.", email);
		return jpaRepository.findByEmailAndStatus(email, UserStatus.VERIFIED.name())
			.map(userEntityMapper::toUser)
			.orElseThrow(() -> new UserException(NOT_FOUND,
				String.format("Verified user with email: %s not found.", email)));
	}

	@Override
	public User findByVerificationCode(String verificationCode) {
		log.debug("Finding user by verificationCode: {}.", verificationCode);
		return jpaRepository.findByVerificationCode(verificationCode)
			.map(userEntityMapper::toUser)
			.orElseThrow(() -> new UserException(NOT_FOUND,
				String.format("User with verificationCode: %s not found.", verificationCode)));
	}
}
