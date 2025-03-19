package com.comex.usermodule.core.service;

import static com.comex.usermodule.core.exception.UserExceptionKey.JWT_TOKEN_EXPIRED;
import static com.comex.usermodule.core.exception.UserExceptionKey.JWT_TOKEN_INVALID;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.exception.UserExceptionKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtService {

	private String jwtSecretKey;
	private Long jwtExpiration;
	private JwtParser jwtParser;

	public JwtService(String jwtSecretKey, Long jwtExpiration) {
		this.jwtSecretKey = jwtSecretKey;
		this.jwtExpiration = jwtExpiration;
		this.jwtParser = Jwts.parser()
			.verifyWith(getSignInKey())
			.build();
	}

	public String generateToken(UserDetails userDetails) {
		log.info("Generating JWT token for user: {}.", userDetails.getUsername());
		return Jwts
			.builder()
			.claims(Map.of("roles", userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","))))
			.subject(userDetails.getUsername())
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + jwtExpiration))
			.signWith(getSignInKey())
			.compact();
	}

	public Claims extractAllClaims(String token) {

		Claims claims;
		try {
			claims = jwtParser
				.parseSignedClaims(token)
				.getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			log.error("Token: {} is invalid.", token, e);
			throw new UserException(JWT_TOKEN_INVALID, String.format("Token: %s is invalid.", token));
		}

		if (isTokenExpired(claims)) {
			throw new UserException(JWT_TOKEN_EXPIRED, String.format("Token: %s is expired.", token));
		}
		return claims;
	}

	private Boolean isTokenExpired(Claims claims) {
		return claims.getExpiration().before(new Date());
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
