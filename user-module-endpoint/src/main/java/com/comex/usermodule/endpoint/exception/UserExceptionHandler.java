package com.comex.usermodule.endpoint.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import com.comex.usermodule.endpoint.controller.UserController;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps a {@link UserException} from the module's own endpoints to a status instead of a 500: an unknown user or
 * verification code is {@code 404}, anything else {@code 400}. Scoped to {@link UserController}, so it never takes
 * over an application's own exception handling.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {

	@ExceptionHandler(UserException.class)
	public ResponseEntity<Map<String, String>> handle(UserException exception) {
		HttpStatus status = exception.getErrorKey() == UserExceptionKey.NOT_FOUND
			? HttpStatus.NOT_FOUND
			: HttpStatus.BAD_REQUEST;
		log.warn("User endpoint rejected the request: {}", exception.getMessage());
		return ResponseEntity.status(status).body(Map.of("error", exception.getErrorKey().name()));
	}
}
