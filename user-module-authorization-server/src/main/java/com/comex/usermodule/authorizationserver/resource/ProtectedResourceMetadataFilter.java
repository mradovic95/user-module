package com.comex.usermodule.authorizationserver.resource;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serves the RFC 9728 protected resource metadata documents at {@code /.well-known/oauth-protected-resource} and
 * {@code /.well-known/oauth-protected-resource/<resource path>}.
 */
@RequiredArgsConstructor
public class ProtectedResourceMetadataFilter extends OncePerRequestFilter {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ProtectedResources protectedResources;

	public static RequestMatcher requestMatcher() {
		PathPatternRequestMatcher.Builder matchers = PathPatternRequestMatcher.withDefaults();
		return new OrRequestMatcher(
			matchers.matcher(HttpMethod.GET, ProtectedResources.WELL_KNOWN_PATH),
			matchers.matcher(HttpMethod.GET, ProtectedResources.WELL_KNOWN_PATH + "/**"));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		if (!HttpMethod.GET.matches(request.getMethod()) || !requestPath(request)
			.startsWith(ProtectedResources.WELL_KNOWN_PATH)) {
			filterChain.doFilter(request, response);
			return;
		}
		Optional<String> path = protectedResources.pathForMetadataRequest(requestPath(request));
		if (path.isEmpty()) {
			response.sendError(HttpStatus.NOT_FOUND.value());
			return;
		}
		response.setStatus(HttpStatus.OK.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(JSON.writeValueAsString(protectedResources.metadata(path.get())));
	}

	private static String requestPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
			? uri.substring(contextPath.length())
			: uri;
	}
}
