package com.comex.usermodule.core.port;

import com.comex.usermodule.endpoint.model.LoginUserRequest;

public interface UserAuthenticator {

	String authenticate(LoginUserRequest loginUserRequest);
}
