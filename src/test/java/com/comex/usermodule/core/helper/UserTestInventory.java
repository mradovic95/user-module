package com.comex.usermodule.core.helper;

import com.comex.usermodule.core.domain.Role;
import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.endpoint.model.LoginUserRequest;

import java.time.Instant;
import java.util.Set;

/**
 * Test inventory for User-related test data.
 * Provides factory methods for creating User domain objects and related DTOs.
 */
public class UserTestInventory {

    // Default test constants
    public static final Long DEFAULT_USER_ID = 1L;
    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_USERNAME = "testuser";
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$encodedPassword";
    public static final String DEFAULT_VERIFICATION_CODE = "ABC123";

    // User Factory Methods

    public static User verifiedUser() {
        return User.builder()
                .id(DEFAULT_USER_ID)
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
                .id(DEFAULT_USER_ID)
                .email(DEFAULT_EMAIL)
                .username(DEFAULT_USERNAME)
                .password(DEFAULT_ENCODED_PASSWORD)
                .status(UserStatus.CREATED)
                .verificationCode(DEFAULT_VERIFICATION_CODE)
                .createdAt(Instant.now())
                .roles(Set.of(userRole()))
                .build();
    }

    public static User userWithEmail(String email) {
        return User.builder()
                .id(DEFAULT_USER_ID)
                .email(email)
                .username(DEFAULT_USERNAME)
                .password(DEFAULT_ENCODED_PASSWORD)
                .status(UserStatus.VERIFIED)
                .createdAt(Instant.now())
                .roles(Set.of(userRole()))
                .build();
    }

    public static User.UserBuilder userBuilder() {
        return User.builder()
                .id(DEFAULT_USER_ID)
                .email(DEFAULT_EMAIL)
                .username(DEFAULT_USERNAME)
                .password(DEFAULT_ENCODED_PASSWORD)
                .status(UserStatus.VERIFIED)
                .createdAt(Instant.now())
                .roles(Set.of(userRole()));
    }

    // Role Factory Methods

    public static Role userRole() {
        return new Role("ROLE_USER", Set.of("READ", "WRITE"));
    }

    public static Role adminRole() {
        return new Role("ROLE_ADMIN", Set.of("READ", "WRITE", "DELETE", "ADMIN"));
    }

    // DTO Factory Methods

    public static CreateUserDto createUserDto() {
        return new CreateUserDto(DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_EMAIL);
    }

    public static CreateUserDto createUserDto(String username, String password, String email) {
        return new CreateUserDto(username, password, email);
    }

    public static LoginUserRequest loginRequest() {
        return new LoginUserRequest(DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    public static LoginUserRequest loginRequest(String email, String password) {
        return new LoginUserRequest(email, password);
    }

    private UserTestInventory() {
        // Utility class - prevent instantiation
    }
}
