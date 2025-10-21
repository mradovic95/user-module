package com.comex.usermodule.endpoint.controller;

import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.service.UserAuthenticationService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.core.service.UserVerificationService;
import com.comex.usermodule.endpoint.mapper.UserWebMapper;
import com.comex.usermodule.endpoint.model.CreateUserRequest;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.comex.usermodule.core.exception.UserExceptionKey.NOT_FOUND;
import org.springframework.security.authentication.BadCredentialsException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;

@WebMvcTest(controllers = UserController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
class UserControllerTest {

    @Autowired
    private MockMvc sut;

    @MockBean
    private UserService userService;

    @MockBean
    private UserAuthenticationService userAuthenticationService;

    @MockBean
    private UserVerificationService userVerificationService;

    @MockBean
    private UserWebMapper userWebMapper;

    @Test
    void testCreateUser() throws Exception {
        // GIVEN
        CreateUserDto dto = new CreateUserDto("john_doe", "password123", "john@example.com");
        when(userWebMapper.toCreateUserDto(any(CreateUserRequest.class))).thenReturn(dto);

        // WHEN & THEN
        sut.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "john_doe",
                                    "password": "password123",
                                    "email": "john@example.com"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(userWebMapper).toCreateUserDto(any(CreateUserRequest.class));
        verify(userService).createUser(dto);
    }

    @Test
    void testCreateUserWithNullValues() throws Exception {
        // GIVEN - request with null values
        CreateUserDto dto = new CreateUserDto(null, null, null);
        when(userWebMapper.toCreateUserDto(any(CreateUserRequest.class))).thenReturn(dto);

        // WHEN & THEN
        sut.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        verify(userWebMapper).toCreateUserDto(any(CreateUserRequest.class));
        verify(userService).createUser(dto);
    }

    @Test
    void testLogin() throws Exception {
        // GIVEN
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIn0.xyz";
        when(userAuthenticationService.login(any(LoginUserRequest.class)))
                .thenReturn(expectedToken);

        // WHEN & THEN
        sut.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "john@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken));

        verify(userAuthenticationService).login(any(LoginUserRequest.class));
    }

    @Test
    void testVerify() throws Exception {
        // GIVEN
        String verificationCode = "ABC123";

        // WHEN & THEN
        sut.perform(get("/user/verify")
                        .param("code", verificationCode))
                .andExpect(status().isOk());

        verify(userVerificationService).verify(verificationCode);
    }
}
