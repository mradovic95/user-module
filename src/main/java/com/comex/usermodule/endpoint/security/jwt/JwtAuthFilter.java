package com.comex.usermodule.endpoint.security.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.comex.usermodule.core.service.JwtService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String authorizationHeader = request.getHeader("Authorization");

		log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
		log.debug("Authorization header: {}", authorizationHeader);

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			log.debug("No Bearer token found, continuing filter chain...");
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(7);
		log.debug("Extracted JWT token: {}", token);

		try {
			Claims claims = jwtService.extractAllClaims(token);
			log.debug("Extracted claims: {}", claims);

			String username = claims.getSubject();
			log.debug("Username from token: {}", username);

			List<GrantedAuthority> authorities = Arrays.stream(((String) claims.get("roles")).split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

			log.debug("Extracted roles: {}", authorities);

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(username, null, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.debug("Authentication set in SecurityContext for user: {}", username);
			}
		} catch (Exception e) {
			log.error("JWT processing error: {}", e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("Invalid or expired JWT token.");
			return;
		}

		log.debug("JWT validated successfully, continuing filter chain...");
		filterChain.doFilter(request, response);
	}

}
