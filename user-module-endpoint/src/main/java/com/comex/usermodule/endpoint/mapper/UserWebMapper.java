package com.comex.usermodule.endpoint.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.endpoint.model.CreateUserRequest;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import com.comex.usermodule.endpoint.model.UserResponse;

@Component
public class UserWebMapper {

	public CreateUserDto toCreateUserDto(CreateUserRequest createUserRequest) {
		return new CreateUserDto(createUserRequest.username(), createUserRequest.password(), createUserRequest.email());
	}

	public LoginUserDto toLoginUserDto(LoginUserRequest loginUserRequest) {
		return new LoginUserDto(loginUserRequest.email(), loginUserRequest.password());
	}

	public UserResponse toUserResponse(User user) {
		return new UserResponse(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			user.getStatus().name(),
			user.getCreatedAt(),
			user.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet())
		);
	}
}
