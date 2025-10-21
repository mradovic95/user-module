package com.comex.usermodule.core.service;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.Date;
import java.util.Set;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService sut;

    private static final String TEST_SECRET_KEY = Base64.getEncoder().encodeToString(
            "ThisIsAVerySecureSecretKeyForTestingPurposesOnly123456789".getBytes()
    );
    private static final Long TEST_EXPIRATION = 3600000L;
    private static final Long SHORT_EXPIRATION = 100L;

    @BeforeEach
    void setUp() {
        sut = new JwtService(TEST_SECRET_KEY, TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        // GIVEN
        User user = userBuilder()
                .username("testuser")
                .roles(Set.of(userRole(), adminRole()))
                .build();
        long beforeGeneration = System.currentTimeMillis();

        // WHEN
        String token = sut.generateToken(user);
        Claims claims = sut.extractAllClaims(token);
        long afterGeneration = System.currentTimeMillis();

        // THEN
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);

        assertThat(claims.getSubject()).isEqualTo("testuser");

        String roles = (String) claims.get("roles");
        assertThat(roles).contains("ROLE_USER");
        assertThat(roles).contains("ROLE_ADMIN");

        assertThat(claims.getIssuedAt()).isNotNull();

        assertThat(claims.getExpiration()).isNotNull();
        long expectedExpiration = beforeGeneration + TEST_EXPIRATION;
        assertThat(claims.getExpiration().getTime()).isCloseTo(expectedExpiration, within(1000L));
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void testExtractAllClaims() {
        // GIVEN
        User user = verifiedUser();
        String token = sut.generateToken(user);

        // WHEN
        Claims claims = sut.extractAllClaims(token);

        // THEN
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(user.getUsername());
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.jwt.token", "not-a-jwt-token"})
    void testExtractAllClaimsThrowsExceptionForInvalidToken(String invalidToken) {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> sut.extractAllClaims(invalidToken))
                .isInstanceOf(UserException.class)
                .extracting("errorKey")
                .isEqualTo(UserExceptionKey.JWT_TOKEN_INVALID);
    }

    @Test
    void testExtractAllClaimsThrowsExceptionForTokenWithDifferentKey() {
        // GIVEN
        String differentSecretKey = Base64.getEncoder().encodeToString(
                "DifferentSecretKeyForTestingPurposesOnly987654321".getBytes()
        );
        JwtService differentKeyService = new JwtService(differentSecretKey, TEST_EXPIRATION);
        User user = verifiedUser();
        String token = differentKeyService.generateToken(user);

        // WHEN / THEN
        assertThatThrownBy(() -> sut.extractAllClaims(token))
                .isInstanceOf(UserException.class)
                .extracting("errorKey")
                .isEqualTo(UserExceptionKey.JWT_TOKEN_INVALID);
    }

    @Test
    void testExtractAllClaimsThrowsExceptionForExpiredToken() throws InterruptedException {
        // GIVEN
        JwtService shortExpirationService = new JwtService(TEST_SECRET_KEY, SHORT_EXPIRATION);
        User user = verifiedUser();
        String token = shortExpirationService.generateToken(user);

        Thread.sleep(200);

        // WHEN / THEN
        assertThatThrownBy(() -> shortExpirationService.extractAllClaims(token))
                .isInstanceOf(UserException.class)
                .extracting("errorKey")
                .isEqualTo(UserExceptionKey.JWT_TOKEN_INVALID);
    }
}
