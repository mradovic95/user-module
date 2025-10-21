package com.comex.usermodule.endpoint.security;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.dto.LoginUserOAuth2Request;
import com.comex.usermodule.core.service.JwtService;
import com.comex.usermodule.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.comex.usermodule.core.helper.UserTestInventory.verifiedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGoogleSpringAuthenticatorTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    private UserGoogleSpringAuthenticator sut;

    @BeforeEach
    void setUp() {
        sut = new UserGoogleSpringAuthenticator(userService, jwtService);
    }

    @Test
    void testAuthenticateExistingUser() {
        // GIVEN
        LoginUserOAuth2Request request = new LoginUserOAuth2Request("john@example.com", "John Doe");
        User existingUser = verifiedUser();
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";

        when(userService.findByEmailOptional(request.email())).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn(expectedToken);

        // WHEN
        String actualToken = sut.authenticate(request);

        // THEN
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(userService).findByEmailOptional(request.email());
        verify(jwtService).generateToken(existingUser);
    }

    @Test
    void testAuthenticateNewUser() {
        // GIVEN
        String email = "jane@example.com";
        String name = "Jane Doe";
        LoginUserOAuth2Request request = new LoginUserOAuth2Request(email, name);
        User newUser = verifiedUser();
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.abc";

        when(userService.findByEmailOptional(request.email())).thenReturn(Optional.empty());
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(newUser);
        when(jwtService.generateToken(newUser)).thenReturn(expectedToken);

        // WHEN
        String actualToken = sut.authenticate(request);

        // THEN
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(userService).findByEmailOptional(request.email());
        verify(userService).createUser(
                argThat(dto -> dto.email().equals(email) &&
                        dto.email().equals(dto.username())));
        verify(jwtService).generateToken(newUser);
    }
}
