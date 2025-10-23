package com.comex.usermodule.core.service;

import com.comex.usermodule.core.dto.LoginUserDto;
import com.comex.usermodule.core.port.UserAuthenticator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.authentication.BadCredentialsException;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @Mock
    private UserAuthenticator userAuthenticator;

    @InjectMocks
    private UserAuthenticationService sut;

    private static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.test";

    @Test
    void testLogin() {
        // GIVEN
        LoginUserDto loginUserDto = loginUserDto(DEFAULT_EMAIL, DEFAULT_PASSWORD);

        when(userAuthenticator.authenticate(loginUserDto)).thenReturn(VALID_JWT_TOKEN);

        // WHEN
        String token = sut.login(loginUserDto);

        // THEN
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo(VALID_JWT_TOKEN);
        verify(userAuthenticator, times(1)).authenticate(loginUserDto);
    }

//    @Test
//    void testLoginThrowsExceptionWhenAuthenticationFails() {
//        // GIVEN
//        LoginUserDto loginUserDto = loginUserDto(DEFAULT_EMAIL, "wrongpassword");
//        BadCredentialsException expectedException = new BadCredentialsException("Invalid credentials");
//
//        when(userAuthenticator.authenticate(loginUserDto)).thenThrow(expectedException);
//
//        // WHEN / THEN
//        assertThatThrownBy(() -> sut.login(loginUserDto))
//                .isInstanceOf(BadCredentialsException.class)
//                .hasMessage("Invalid credentials");
//
//        verify(userAuthenticator, times(1)).authenticate(loginUserDto);
//    }
}
