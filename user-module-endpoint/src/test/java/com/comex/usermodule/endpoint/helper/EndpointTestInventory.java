package com.comex.usermodule.endpoint.helper;

import com.comex.usermodule.endpoint.model.CreateUserRequest;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import com.comex.usermodule.endpoint.model.UserResponse;

import java.time.Instant;
import java.util.Set;

import static com.comex.usermodule.core.helper.UserTestInventory.*;

/**
 * Test inventory for Endpoint-related test data.
 * Provides factory methods for creating Request/Response models.
 */
public class EndpointTestInventory {

	// Request Factory Methods

	public static CreateUserRequest createUserRequest() {
		return new CreateUserRequest(DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_EMAIL);
	}

	public static CreateUserRequest createUserRequest(String username, String password, String email) {
		return new CreateUserRequest(username, password, email);
	}

	public static LoginUserRequest loginUserRequest() {
		return new LoginUserRequest(DEFAULT_EMAIL, DEFAULT_PASSWORD);
	}

	public static LoginUserRequest loginUserRequest(String email, String password) {
		return new LoginUserRequest(email, password);
	}

	// Response Factory Methods

	public static UserResponse verifiedUserResponse() {
		return new UserResponse(
			DEFAULT_USER_ID,
			DEFAULT_USERNAME,
			DEFAULT_EMAIL,
			"VERIFIED",
			Instant.now(),
			Set.of("ROLE_USER")
		);
	}

	public static UserResponse userResponse(Long id, String username, String email, String status, Instant createdAt, Set<String> roles) {
		return new UserResponse(id, username, email, status, createdAt, roles);
	}

	// JWT Token
	public static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.xyz";

	private EndpointTestInventory() {
		// Utility class - prevent instantiation
	}
}
