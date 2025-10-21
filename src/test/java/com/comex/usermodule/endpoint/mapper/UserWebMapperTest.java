package com.comex.usermodule.endpoint.mapper;

import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.endpoint.model.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserWebMapperTest {

    private UserWebMapper sut;

    @BeforeEach
    void setUp() {
        sut = new UserWebMapper();
    }

    @Test
    void testToCreateUserDto() {
        // GIVEN
        String username = "john_doe";
        String password = "password123";
        String email = "john@example.com";
        CreateUserRequest request = new CreateUserRequest(username, password, email);

        // WHEN
        CreateUserDto dto = sut.toCreateUserDto(request);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isEqualTo(username);
        assertThat(dto.password()).isEqualTo(password);
        assertThat(dto.email()).isEqualTo(email);
    }

    @Test
    void testToCreateUserDtoWithNullValues() {
        // GIVEN
        CreateUserRequest request = new CreateUserRequest(null, null, null);

        // WHEN
        CreateUserDto dto = sut.toCreateUserDto(request);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isNull();
        assertThat(dto.password()).isNull();
        assertThat(dto.email()).isNull();
    }
}
