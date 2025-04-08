package com.comex.usermodule.infrastructure.persistence.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.infrastructure.persistence.entity.PermissionEntity;
import com.comex.usermodule.infrastructure.persistence.entity.RoleEntity;
import com.comex.usermodule.infrastructure.persistence.entity.UserEntity;

public class UserEntityMapper {

	public UserEntity toUserEntity(User user, Set<RoleEntity> roles) {
		return UserEntity.builder()
			.id(user.getId())
			.username(user.getUsername())
			.password(user.getPassword())
			.email(user.getEmail())
			.createdAt(user.getCreatedAt())
			.status(user.getStatus().name())
			.roles(roles)
			.verificationCode(user.getVerificationCode())
			.build();
	}

	public User toUser(UserEntity userEntity) {
		return User.builder()
			.id(userEntity.getId())
			.username(userEntity.getUsername())
			.password(userEntity.getPassword())
			.email(userEntity.getEmail())
			.createdAt(userEntity.getCreatedAt())
			.roles(userEntity.getRoles().
				stream()
				.map(this::toRole)
				.collect(Collectors.toSet()))
			.status(UserStatus.valueOf(userEntity.getStatus()))
			.verificationCode(userEntity.getVerificationCode())
			.build();
	}

	private Role toRole(RoleEntity roleEntity) {

		return new Role(roleEntity.getName(), roleEntity.getPermissions().stream()
			.map(PermissionEntity::getName)
			.collect(Collectors.toSet()));
	}
}
