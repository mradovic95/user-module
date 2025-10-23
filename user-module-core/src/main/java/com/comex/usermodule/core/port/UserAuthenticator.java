package com.comex.usermodule.core.port;

import com.comex.usermodule.core.dto.LoginUserDto;

public interface UserAuthenticator {

	String authenticate(LoginUserDto loginUserDto);
}
