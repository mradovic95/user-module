package com.comex.usermodule.core.domain;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Role {

	private String name;
	private Set<String> permissions;

	public Role(String name) {
		this.name = name;
	}

	public Collection<? extends GrantedAuthority> getAuthorities() {
		Set<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
		// add role to authority authorities
		authorities.add(new SimpleGrantedAuthority(name));
		// add all permissions to  authorities
		authorities.addAll(permissions.stream()
			.map(SimpleGrantedAuthority::new)
			.collect(Collectors.toSet()));
		return authorities;
	}
}
