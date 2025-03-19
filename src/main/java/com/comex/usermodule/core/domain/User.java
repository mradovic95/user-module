package com.comex.usermodule.core.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User implements UserDetails {

	private Long id;
	private String username;
	private String password;
	private Set<Role> roles;
	private String email;
	private Instant createdAt;
	private UserStatus status;
	private String verificationCode;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// return authorities for user
		return roles.stream()
			.flatMap(r -> r.getAuthorities().stream())
			.collect(Collectors.toSet());
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}
}
