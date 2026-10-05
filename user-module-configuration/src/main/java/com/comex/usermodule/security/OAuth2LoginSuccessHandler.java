package com.comex.usermodule.security;

import static com.comex.usermodule.core.exception.UserExceptionKey.OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.comex.usermodule.core.dto.LoginUserOAuth2Dto;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.port.UserGoogleAuthenticator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Completes an OAuth2 login by exchanging the provider identity for a module JWT.
 * <p>
 * Success: when a success redirect URL is configured the browser is redirected there with the JWT as a
 * {@code token} query parameter; otherwise the JWT is written to the response body as JSON.
 * <p>
 * Rejection (missing email, unverified email, email domain not allowed): with a redirect URL configured the
 * browser is redirected there with an {@code error} query parameter; otherwise a JSON error body is written with
 * the matching HTTP status.
 */
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	static final String TOKEN_PARAMETER = "token";
	static final String ERROR_PARAMETER = "error";
	static final String ERROR_EMAIL_MISSING = "email_missing";
	static final String ERROR_EMAIL_NOT_VERIFIED = "email_not_verified";
	static final String ERROR_DOMAIN_NOT_ALLOWED = "domain_not_allowed";

	private final UserGoogleAuthenticator userGoogleAuthenticator;
	private final String successRedirectUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		if (!StringUtils.hasText(email)) {
			log.warn("OAuth2 login rejected: the provider returned no email attribute.");
			reject(request, response, HttpServletResponse.SC_UNAUTHORIZED, ERROR_EMAIL_MISSING,
				"OAuth2 provider did not return an email");
			return;
		}

		Boolean emailVerified = oAuth2User.getAttribute("email_verified");
		if (Boolean.FALSE.equals(emailVerified)) {
			log.warn("OAuth2 login rejected for {}: email is not verified by the provider.", email);
			reject(request, response, HttpServletResponse.SC_UNAUTHORIZED, ERROR_EMAIL_NOT_VERIFIED,
				"OAuth2 provider reports the email as not verified");
			return;
		}

		String jwt;
		try {
			jwt = userGoogleAuthenticator.authenticate(new LoginUserOAuth2Dto(email, oAuth2User.getAttribute("name")));
		} catch (UserException e) {
			if (e.getErrorKey() != OAUTH2_EMAIL_DOMAIN_NOT_ALLOWED) {
				throw e;
			}
			reject(request, response, HttpServletResponse.SC_FORBIDDEN, ERROR_DOMAIN_NOT_ALLOWED,
				"Email domain is not allowed to sign in");
			return;
		}
		log.debug("OAuth2 login completed for {}.", email);

		if (StringUtils.hasText(successRedirectUrl)) {
			getRedirectStrategy().sendRedirect(request, response, redirectUrl(TOKEN_PARAMETER, jwt));
			return;
		}

		writeJson(response, "{\"token\": \"" + jwt + "\"}");
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, int status, String code,
		String message) throws IOException {
		if (StringUtils.hasText(successRedirectUrl)) {
			getRedirectStrategy().sendRedirect(request, response, redirectUrl(ERROR_PARAMETER, code));
			return;
		}
		response.setStatus(status);
		writeJson(response, "{\"error\": \"" + message + "\"}");
	}

	private String redirectUrl(String parameter, String value) {
		return UriComponentsBuilder.fromUriString(successRedirectUrl)
			.queryParam(parameter, value)
			.build()
			.toUriString();
	}

	private static void writeJson(HttpServletResponse response, String body) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(body);
	}
}
