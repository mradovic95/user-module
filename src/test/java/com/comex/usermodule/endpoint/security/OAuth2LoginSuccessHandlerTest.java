package com.comex.usermodule.endpoint.security;

import com.comex.usermodule.core.dto.LoginUserOAuth2Request;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserGoogleAuthenticator userGoogleAuthenticator;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2LoginSuccessHandler sut;

    @BeforeEach
    void setUp() {
        sut = new OAuth2LoginSuccessHandler(userGoogleAuthenticator);
    }

    @Test
    void testOnAuthenticationSuccess() throws Exception {
        // GIVEN
        String email = "john@example.com";
        String name = "John Doe";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(userGoogleAuthenticator.authenticate(any(LoginUserOAuth2Request.class)))
                .thenReturn(expectedToken);
        when(response.getWriter()).thenReturn(writer);

        // WHEN
        sut.onAuthenticationSuccess(request, response, authentication);

        // THEN
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        verify(userGoogleAuthenticator).authenticate(
                argThat(req -> req.email().equals(email) && req.name().equals(name)));

        writer.flush();
        String output = stringWriter.toString();
        assertThat(output).isEqualTo("{\"token\": \"" + expectedToken + "\"}");
    }
}
