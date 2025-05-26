package com.comex.usermodule.endpoint.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import com.comex.usermodule.core.dto.LoginUserOAuth2Request;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UserGoogleAuthenticator userGoogleAuthenticator;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

		String jwt = userGoogleAuthenticator.authenticate(
			new LoginUserOAuth2Request(oAuth2User.getAttribute("email"), oAuth2User.getAttribute("name")));

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"token\": \"" + jwt + "\"}");
	}
}
