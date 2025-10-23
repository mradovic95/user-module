package com.comex.usermodule.core.port;

import java.util.Optional;

import com.comex.usermodule.core.domain.User;

public interface UserRepository {

	User save(User user);

	User findByEmail(String email);

	Optional<User> findByEmailOptional(String email);

	User findByVerificationCode(String verificationCode);
}
