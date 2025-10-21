package com.comex.usermodule.core.service;

import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import com.comex.usermodule.core.port.UserAuthenticator;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

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
        LoginUserRequest loginRequest = loginRequest(DEFAULT_EMAIL, DEFAULT_PASSWORD);

        when(userAuthenticator.authenticate(loginRequest)).thenReturn(VALID_JWT_TOKEN);

        // WHEN
        String token = sut.login(loginRequest);

        // THEN
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo(VALID_JWT_TOKEN);
        verify(userAuthenticator, times(1)).authenticate(loginRequest);
    }

    @Test
    void testLoginThrowsExceptionWhenAuthenticationFails() {
        // GIVEN
        LoginUserRequest loginRequest = loginRequest(DEFAULT_EMAIL, "wrongpassword");
        BadCredentialsException expectedException = new BadCredentialsException("Invalid credentials");

        when(userAuthenticator.authenticate(loginRequest)).thenThrow(expectedException);

        // WHEN / THEN
        assertThatThrownBy(() -> sut.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(userAuthenticator, times(1)).authenticate(loginRequest);
    }
}
