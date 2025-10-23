package com.comex.usermodule.infrastructure.persistence.dynamodb.repository;

import static com.comex.usermodule.core.exception.UserExceptionKey.NOT_FOUND;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.RoleDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.UserDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.mapper.UserDynamoEntityMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
@RequiredArgsConstructor
public class UserDynamoRepository implements UserRepository {

	private final DynamoDbEnhancedClient enhancedClient;
	private final String tableName;
	private final UserDynamoEntityMapper userDynamoEntityMapper;
	private final Map<String, RoleDynamoEntity> roleCache;

	@Override
	public User save(User user) {
		log.debug("Saving user {}.", user);

		// Get roles from cache or create new ones
		List<RoleDynamoEntity> roles = user.getRoles()
			.stream()
			.map(role -> roleCache.computeIfAbsent(role.getName(),
				key -> userDynamoEntityMapper.toRoleDynamoEntity(role)))
			.collect(Collectors.toList());

		// Map and save user
		UserDynamoEntity userDynamoEntity = userDynamoEntityMapper.toUserDynamoEntity(user, roles);
		DynamoDbTable<UserDynamoEntity> table = getTable();
		table.putItem(userDynamoEntity);

		return userDynamoEntityMapper.toUser(userDynamoEntity);
	}

	@Override
	public User findByEmail(String email) {
		log.debug("Finding verified user by email: {}.", email);
		return findByEmailOptional(email)
			.orElseThrow(() -> new UserException(NOT_FOUND,
				String.format("Verified user with email: %s not found.", email)));
	}

	@Override
	public Optional<User> findByEmailOptional(String email) {
		log.debug("Finding verified user by email: {}.", email);
		DynamoDbTable<UserDynamoEntity> table = getTable();

		Key key = Key.builder()
			.partitionValue(email)
			.build();

		UserDynamoEntity userDynamoEntity = table.getItem(key);

		if (userDynamoEntity == null) {
			return Optional.empty();
		}

		// Check if user is verified
		if (!UserStatus.VERIFIED.name().equals(userDynamoEntity.getStatus())) {
			return Optional.empty();
		}

		return Optional.of(userDynamoEntityMapper.toUser(userDynamoEntity));
	}

	@Override
	public User findByVerificationCode(String verificationCode) {
		log.debug("Finding user by verificationCode: {}.", verificationCode);

		DynamoDbTable<UserDynamoEntity> table = getTable();
		DynamoDbIndex<UserDynamoEntity> index = table.index("verification-code-index");

		QueryConditional queryConditional = QueryConditional
			.keyEqualTo(Key.builder()
				.partitionValue(verificationCode)
				.build());

		UserDynamoEntity userDynamoEntity = index.query(queryConditional)
			.stream()
			.flatMap(page -> page.items().stream())
			.findFirst()
			.orElseThrow(() -> new UserException(NOT_FOUND,
				String.format("User with verificationCode: %s not found.", verificationCode)));

		return userDynamoEntityMapper.toUser(userDynamoEntity);
	}

	private DynamoDbTable<UserDynamoEntity> getTable() {
		return enhancedClient.table(tableName, TableSchema.fromBean(UserDynamoEntity.class));
	}
}
