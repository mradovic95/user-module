package com.comex.usermodule.core.port;

import com.comex.usermodule.core.domain.User;

public interface UserRepository {

	User save(User user);

	User findByEmail(String email);

	User findByVerificationCode(String verificationCode);
}
