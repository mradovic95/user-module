package com.comex.usermodule.core.domain;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {

	private Long id;
	private String username;
	private String password;
	private Set<Role> roles;
	private String email;
	private Instant createdAt;
	private UserStatus status;
	private String verificationCode;

	public Set<String> getAuthorities() {
		return roles.stream()
			.flatMap(r -> r.getAuthorities().stream())
			.collect(Collectors.toSet());
	}
}
