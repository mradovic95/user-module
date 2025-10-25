package com.comex.usermodule.endpoint.model;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
	Long id,
	String username,
	String email,
	String status,
	Instant createdAt,
	Set<String> roles
) {
}
