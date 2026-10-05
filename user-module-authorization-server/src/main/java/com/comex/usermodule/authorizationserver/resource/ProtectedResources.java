package com.comex.usermodule.authorizationserver.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The protected resources (e.g. an MCP endpoint) this authorization server issues tokens for, all living under the
 * issuer URL. Produces the RFC 9728 metadata documents and answers audience checks.
 */
public final class ProtectedResources {

	public static final String WELL_KNOWN_PATH = "/.well-known/oauth-protected-resource";

	private final String issuer;
	private final List<String> paths;
	private final List<String> scopes;

	private ProtectedResources(String issuer, List<String> paths, List<String> scopes) {
		this.issuer = issuer;
		this.paths = paths;
		this.scopes = scopes;
	}

	public static ProtectedResources of(String issuer, List<String> paths, List<String> scopes) {
		Assert.hasText(issuer, "issuer must not be blank");
		String normalizedIssuer = stripTrailingSlash(issuer.trim());
		List<String> normalizedPaths = paths == null ? List.of()
			: paths.stream().map(ProtectedResources::normalizePath).distinct().toList();
		List<String> normalizedScopes = scopes == null ? List.of()
			: scopes.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
		return new ProtectedResources(normalizedIssuer, normalizedPaths, normalizedScopes);
	}

	public String issuer() {
		return issuer;
	}

	/** Normalized resource paths, e.g. {@code /mcp}. Empty when the issuer itself is the only resource. */
	public List<String> paths() {
		return paths;
	}

	public List<String> scopes() {
		return scopes;
	}

	/** The first configured path, or {@code ""} (the issuer itself) when none is configured. */
	public String defaultPath() {
		return paths.isEmpty() ? "" : paths.get(0);
	}

	public String resourceUri(String path) {
		return issuer + normalizePath(path);
	}

	public List<String> resourceUris() {
		return paths.isEmpty() ? List.of(issuer) : paths.stream().map(this::resourceUri).toList();
	}

	public String metadataUrl(String path) {
		return issuer + WELL_KNOWN_PATH + normalizePath(path);
	}

	/**
	 * Resolves the resource path a metadata request is about: {@code /.well-known/oauth-protected-resource}
	 * describes the default resource, {@code /.well-known/oauth-protected-resource/<path>} a configured one.
	 */
	public Optional<String> pathForMetadataRequest(String requestPath) {
		if (requestPath == null || !requestPath.startsWith(WELL_KNOWN_PATH)) {
			return Optional.empty();
		}
		String remainder = normalizePath(requestPath.substring(WELL_KNOWN_PATH.length()));
		if (remainder.isEmpty()) {
			return Optional.of(defaultPath());
		}
		return paths.contains(remainder) ? Optional.of(remainder) : Optional.empty();
	}

	public Map<String, Object> metadata(String path) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("resource", resourceUri(path));
		metadata.put("authorization_servers", List.of(issuer));
		metadata.put("bearer_methods_supported", List.of("header"));
		if (!scopes.isEmpty()) {
			metadata.put("scopes_supported", scopes);
		}
		return metadata;
	}

	/** Whether a token audience value denotes one of the configured resources (trailing slash and case tolerant). */
	public boolean isAudience(String audience) {
		if (!StringUtils.hasText(audience)) {
			return false;
		}
		String normalized = normalizeUri(audience);
		return resourceUris().stream().map(ProtectedResources::normalizeUri).anyMatch(normalized::equals);
	}

	public BearerResourceMetadataEntryPoint entryPoint(String path) {
		return new BearerResourceMetadataEntryPoint(metadataUrl(path), scopes);
	}

	static String normalizePath(String path) {
		if (!StringUtils.hasText(path)) {
			return "";
		}
		String trimmed = path.trim();
		if (!trimmed.startsWith("/")) {
			trimmed = "/" + trimmed;
		}
		return stripTrailingSlash(trimmed);
	}

	static String normalizeUri(String uri) {
		String trimmed = stripTrailingSlash(uri.trim());
		try {
			URI parsed = URI.create(trimmed);
			if (parsed.getScheme() == null || parsed.getHost() == null) {
				return trimmed;
			}
			String authority = parsed.getHost().toLowerCase(Locale.ROOT)
				+ (parsed.getPort() == -1 ? "" : ":" + parsed.getPort());
			String path = parsed.getRawPath() == null ? "" : parsed.getRawPath();
			return parsed.getScheme().toLowerCase(Locale.ROOT) + "://" + authority + path;
		} catch (IllegalArgumentException e) {
			return trimmed;
		}
	}

	private static String stripTrailingSlash(String value) {
		String result = value;
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}
}
