package com.comex.usermodule.endpoint.security;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.endpoint.model.LoginUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSpringAuthenticatorTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    private UserSpringAuthenticator sut;

    @BeforeEach
    void setUp() {
        sut = new UserSpringAuthenticator(authenticationManager, jwtService);
    }

    @Test
    void testAuthenticate() {
        // GIVEN
        String email = "john@example.com";
        String password = "password123";
        LoginUserRequest request = new LoginUserRequest(email, password);
        User user = verifiedUser();
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(expectedToken);

        // WHEN
        String actualToken = sut.authenticate(request);

        // THEN
        assertThat(actualToken).isNotNull();
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken &&
                        auth.getPrincipal().equals(email) &&
                        auth.getCredentials().equals(password)));
        verify(jwtService).generateToken(user);
    }
}
