package com.comex.usermodule.endpoint.mapper;

import org.springframework.stereotype.Component;

import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.endpoint.model.CreateUserRequest;
import com.comex.usermodule.endpoint.model.LoginUserRequest;

@Component
public class UserWebMapper {

	public CreateUserDto toCreateUserDto(CreateUserRequest createUserRequest) {
		return new CreateUserDto(createUserRequest.username(), createUserRequest.password(), createUserRequest.email());
	}

	public LoginUserDto toLoginUserDto(LoginUserRequest loginUserRequest) {
		return new LoginUserDto(loginUserRequest.email(), loginUserRequest.password());
	}
}
