package com.comex.usermodule.endpoint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.core.service.UserAuthenticationService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.core.service.UserVerificationService;
import com.comex.usermodule.endpoint.mapper.UserWebMapper;
import com.comex.usermodule.endpoint.model.CreateUserRequest;
import com.comex.usermodule.endpoint.model.LoginTokenResponse;
import com.comex.usermodule.endpoint.model.LoginUserRequest;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class UserController {

	private final UserService userService;
	private final UserAuthenticationService userAuthenticationService;
	private final UserVerificationService userVerificationService;
	private final UserWebMapper userWebMapper;

	@PostMapping
	public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest createUserRequest) {
		userService.createUser(userWebMapper.toCreateUserDto(createUserRequest));
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginTokenResponse> login(@RequestBody LoginUserRequest loginUserRequest) {
		LoginUserDto loginUserDto = userWebMapper.toLoginUserDto(loginUserRequest);
		return ResponseEntity.ok(new LoginTokenResponse(userAuthenticationService.login(loginUserDto)));
	}

	@GetMapping("/verify")
	public ResponseEntity<Void> login(@RequestParam("code") String code) {
		userVerificationService.verify(code);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
