package com.comex.usermodule.core.port;

public interface PasswordEncoder {

	String encode(CharSequence rawPassword);
}
