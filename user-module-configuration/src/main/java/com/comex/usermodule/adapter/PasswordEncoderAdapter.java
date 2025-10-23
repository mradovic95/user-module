package com.comex.usermodule.adapter;

import com.comex.usermodule.core.port.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordEncoderAdapter implements PasswordEncoder {

	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Override
	public String encode(CharSequence rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
}
