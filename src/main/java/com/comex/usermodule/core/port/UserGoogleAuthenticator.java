package com.comex.usermodule.core.port;

import com.comex.usermodule.core.dto.LoginUserOAuth2Request;

public interface UserGoogleAuthenticator {

	String authenticate(LoginUserOAuth2Request loginUserOAuth2Request);
}
