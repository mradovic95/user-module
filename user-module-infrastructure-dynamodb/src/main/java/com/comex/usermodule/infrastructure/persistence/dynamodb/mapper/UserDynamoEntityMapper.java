package com.comex.usermodule.infrastructure.persistence.dynamodb.mapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.PermissionDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.RoleDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.UserDynamoEntity;

public class UserDynamoEntityMapper {

	public UserDynamoEntity toUserDynamoEntity(User user, List<RoleDynamoEntity> roles) {
		return UserDynamoEntity.builder()
			.id(user.getId() != null ? user.getId().toString() : UUID.randomUUID().toString())
			.username(user.getUsername())
			.password(user.getPassword())
			.email(user.getEmail())
			.createdAt(user.getCreatedAt())
			.status(user.getStatus().name())
			.roles(roles)
			.verificationCode(user.getVerificationCode())
			.build();
	}

	public User toUser(UserDynamoEntity userDynamoEntity) {
		Long id = parseIdToLong(userDynamoEntity.getId());
		return User.builder()
			.id(id)
			.username(userDynamoEntity.getUsername())
			.password(userDynamoEntity.getPassword())
			.email(userDynamoEntity.getEmail())
			.createdAt(userDynamoEntity.getCreatedAt())
			.roles(userDynamoEntity.getRoles()
				.stream()
				.map(this::toRole)
				.collect(Collectors.toSet()))
			.status(UserStatus.valueOf(userDynamoEntity.getStatus()))
			.verificationCode(userDynamoEntity.getVerificationCode())
			.build();
	}

	private Long parseIdToLong(String id) {
		if (id == null) {
			return null;
		}
		try {
			return Long.parseLong(id);
		} catch (NumberFormatException e) {
			// ID is a UUID (DynamoDB-generated), return null for domain model
			return null;
		}
	}

	public RoleDynamoEntity toRoleDynamoEntity(Role role) {
		List<PermissionDynamoEntity> permissionEntities = role.getPermissions() != null
			? role.getPermissions().stream()
				.map(permissionName -> PermissionDynamoEntity.builder()
					.name(permissionName)
					.build())
				.collect(Collectors.toList())
			: List.of();

		return RoleDynamoEntity.builder()
			.name(role.getName())
			.permissions(permissionEntities)
			.build();
	}

	private Role toRole(RoleDynamoEntity roleDynamoEntity) {
		return new Role(
			roleDynamoEntity.getName(),
			roleDynamoEntity.getPermissions()
				.stream()
				.map(PermissionDynamoEntity::getName)
				.collect(Collectors.toSet())
		);
	}
}
