package com.comex.usermodule.infrastructure.persistence.dynamodb.helper;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Test inventory for User-related test data.
 * Provides factory methods for creating User domain objects for integration tests.
 */
public class UserTestInventory {

	// Default test constants
	public static final String DEFAULT_EMAIL = "test@example.com";
	public static final String DEFAULT_USERNAME = "testuser";
	public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$encodedPassword";
	public static final String DEFAULT_VERIFICATION_CODE = "ABC123";

	// User Factory Methods

	public static User verifiedUser() {
		return User.builder()
			.email(DEFAULT_EMAIL)
			.username(DEFAULT_USERNAME)
			.password(DEFAULT_ENCODED_PASSWORD)
			.status(UserStatus.VERIFIED)
			.createdAt(Instant.now())
			.roles(Set.of(userRole()))
			.build();
	}

	public static User pendingUser() {
		return User.builder()
			.email(DEFAULT_EMAIL)
			.username(DEFAULT_USERNAME)
			.password(DEFAULT_ENCODED_PASSWORD)
			.status(UserStatus.CREATED)
			.verificationCode(DEFAULT_VERIFICATION_CODE)
			.createdAt(Instant.now())
			.roles(Set.of(userRole()))
			.build();
	}

	// Role Factory Methods

	public static Role userRole() {
		return new Role("ROLE_USER", Set.of("READ", "WRITE"));
	}

	private UserTestInventory() {
		// Utility class - prevent instantiation
	}
}
