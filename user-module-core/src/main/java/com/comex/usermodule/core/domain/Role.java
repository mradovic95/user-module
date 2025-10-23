package com.comex.usermodule.core.domain;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

	public Set<String> getAuthorities() {
		Set<String> authorities = new HashSet<>();
		authorities.add(this.name);
		authorities.addAll(this.permissions);
		return authorities;
	}
}
