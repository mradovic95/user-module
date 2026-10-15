package com.comex.usermodule.core.port;

import com.comex.usermodule.core.domain.User;

/**
 * Issues the access token a user receives after logging in (password or Google). Every token the module hands out
 * comes from its OAuth 2.1 authorization server, so the implementation lives there.
 */
public interface AccessTokenIssuer {

	String issue(User user);
}
