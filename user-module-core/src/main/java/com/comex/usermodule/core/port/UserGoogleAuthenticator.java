package com.comex.usermodule.core.port;

import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;

public interface UserGoogleAuthenticator {

	String authenticate(LoginUserOAuth2Dto loginUserOAuth2Dto);
}
