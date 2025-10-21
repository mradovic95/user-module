package com.comex.usermodule.endpoint.security.jwt;

import com.comex.usermodule.core.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter sut;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        sut = new JwtAuthFilter(jwtService);
    }

    @Test
    void testDoFilterInternalWithValidToken() throws Exception {
        // GIVEN
        String token = "valid-jwt-token";
        String authHeader = "Bearer " + token;
        Claims claims = Jwts.claims()
                .subject("john@example.com")
                .add("roles", "ROLE_USER,ROLE_ADMIN")
                .build();

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user");
        when(jwtService.extractAllClaims(token)).thenReturn(claims);

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        verify(jwtService).extractAllClaims(token);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("john@example.com");
    }

    @Test
    void testDoFilterInternalWithNoAuthorizationHeader() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user");

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        verify(jwtService, never()).extractAllClaims(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testDoFilterInternalWithInvalidBearerFormat() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user");

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        verify(jwtService, never()).extractAllClaims(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testDoFilterInternalWithInvalidToken() throws Exception {
        // GIVEN
        String token = "invalid-token";
        String authHeader = "Bearer " + token;
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user");
        when(jwtService.extractAllClaims(token)).thenThrow(new RuntimeException("Invalid token"));
        when(response.getWriter()).thenReturn(writer);

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        writer.flush();
        String output = stringWriter.toString();
        assertThat(output).contains("Invalid or expired JWT token");
    }

    @Test
    void testDoFilterInternalExtractsRolesCorrectly() throws Exception {
        // GIVEN
        String token = "valid-token";
        String authHeader = "Bearer " + token;
        Claims claims = Jwts.claims()
                .subject("user@example.com")
                .add("roles", "ROLE_USER,ROLE_MANAGER,ROLE_ADMIN")
                .build();

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/user/create");
        when(jwtService.extractAllClaims(token)).thenReturn(claims);

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .hasSize(3);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalDoesNotOverwriteExistingAuthentication() throws Exception {
        // GIVEN
        String token = "token";
        String authHeader = "Bearer " + token;
        Claims claims = Jwts.claims()
                .subject("user@example.com")
                .add("roles", "ROLE_USER")
                .build();

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user");
        when(jwtService.extractAllClaims(token)).thenReturn(claims);

        // Set existing authentication
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "existing@example.com", null, java.util.Collections.emptyList()));

        // WHEN
        sut.doFilterInternal(request, response, filterChain);

        // THEN
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("existing@example.com");
        verify(filterChain).doFilter(request, response);
    }
}
