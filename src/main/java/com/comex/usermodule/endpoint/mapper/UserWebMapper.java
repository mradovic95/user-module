package com.comex.usermodule.endpoint.mapper;

import org.springframework.stereotype.Component;

import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.endpoint.model.CreateUserRequest;

@Component
public class UserWebMapper {

	public CreateUserDto toCreateUserDto(CreateUserRequest createUserRequest) {
		return new CreateUserDto(createUserRequest.username(), createUserRequest.password(), createUserRequest.email());
	}
}
