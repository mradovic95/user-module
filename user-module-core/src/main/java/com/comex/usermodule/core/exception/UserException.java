package com.comex.usermodule.core.exception;

import static io.jsonwebtoken.lang.Strings.hasText;

import java.util.Collections;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class UserException extends RuntimeException {

	private final UserExceptionKey errorKey;

	private final Map<String, String> params;

	public UserException(@NonNull UserExceptionKey errorKey) {

		this(errorKey, Collections.emptyMap());
	}

	public UserException(@NonNull UserExceptionKey errorKey, String message) {

		this(errorKey, Collections.emptyMap(), message);
	}

	public UserException(@NonNull UserExceptionKey errorKey, String message, Throwable cause) {

		this(errorKey, Collections.emptyMap(), message, cause);
	}

	public UserException(@NonNull UserExceptionKey errorKey, @NonNull Map<String, String> params) {

		this(errorKey, params, null);
	}

	public UserException(@NonNull UserExceptionKey errorKey, @NonNull Map<String, String> params,
		String message) {

		this(errorKey, params, message, null);
	}

	public UserException(@NonNull UserExceptionKey errorKey, @NonNull Map<String, String> params,
		String message, Throwable cause) {

		super("Error Key [" + errorKey.name() + "]" + (hasText(message) ? "; " + message : ""), cause);

		this.errorKey = errorKey;
		this.params = params;
	}
}
