package com.comex.usermodule.authorizationserver.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProtectedResourcesTest {

	private static final String ISSUER = "https://api.example.com";

	private ProtectedResources sut;

	@Test
	void testOfNormalizesIssuerPathsAndScopes() {
		// GIVEN
		// trailing slashes, missing leading slash, duplicates and blanks

		// WHEN
		sut = ProtectedResources.of(ISSUER + "/", List.of("mcp", "/mcp/", "/agent/mcp/"), List.of(" tools ", "", "tools"));

		// THEN
		assertThat(sut.issuer()).isEqualTo(ISSUER);
		assertThat(sut.paths()).containsExactly("/mcp", "/agent/mcp");
		assertThat(sut.scopes()).containsExactly("tools");
		assertThat(sut.defaultPath()).isEqualTo("/mcp");
		assertThat(sut.resourceUris()).containsExactly(ISSUER + "/mcp", ISSUER + "/agent/mcp");
		assertThat(sut.metadataUrl("/mcp")).isEqualTo(ISSUER + "/.well-known/oauth-protected-resource/mcp");
	}

	@Test
	void testWithoutPathsTheIssuerIsTheResource() {
		// GIVEN
		// no resource paths configured

		// WHEN
		sut = ProtectedResources.of(ISSUER, List.of(), null);

		// THEN
		assertThat(sut.defaultPath()).isEmpty();
		assertThat(sut.resourceUris()).containsExactly(ISSUER);
		assertThat(sut.metadataUrl("")).isEqualTo(ISSUER + "/.well-known/oauth-protected-resource");
		assertThat(sut.isAudience(ISSUER + "/")).isTrue();
		assertThat(sut.pathForMetadataRequest("/.well-known/oauth-protected-resource")).contains("");
	}

	@ParameterizedTest
	@CsvSource({
		"/.well-known/oauth-protected-resource, /mcp",
		"/.well-known/oauth-protected-resource/, /mcp",
		"/.well-known/oauth-protected-resource/mcp, /mcp",
		"/.well-known/oauth-protected-resource/mcp/, /mcp",
		"/.well-known/oauth-protected-resource/agent/mcp, /agent/mcp"
	})
	void testPathForMetadataRequestResolvesConfiguredResources(String requestPath, String expectedPath) {
		// GIVEN
		sut = ProtectedResources.of(ISSUER, List.of("/mcp", "/agent/mcp"), List.of());

		// WHEN
		var path = sut.pathForMetadataRequest(requestPath);

		// THEN
		assertThat(path).contains(expectedPath);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/.well-known/oauth-protected-resource/unknown", "/.well-known/other", "/mcp" })
	void testPathForMetadataRequestIsEmptyForUnknownPaths(String requestPath) {
		// GIVEN
		sut = ProtectedResources.of(ISSUER, List.of("/mcp"), List.of());

		// WHEN
		var path = sut.pathForMetadataRequest(requestPath);

		// THEN
		assertThat(path).isEmpty();
	}

	@Test
	void testMetadata() {
		// GIVEN
		sut = ProtectedResources.of(ISSUER, List.of("/mcp"), List.of("tools"));

		// WHEN
		Map<String, Object> metadata = sut.metadata("/mcp");

		// THEN
		assertThat(metadata).containsExactly(
			Map.entry("resource", ISSUER + "/mcp"),
			Map.entry("authorization_servers", List.of(ISSUER)),
			Map.entry("bearer_methods_supported", List.of("header")),
			Map.entry("scopes_supported", List.of("tools")));
		assertThat(ProtectedResources.of(ISSUER, List.of("/mcp"), List.of()).metadata("/mcp"))
			.doesNotContainKey("scopes_supported");
	}

	@ParameterizedTest
	@CsvSource({
		"https://api.example.com/mcp, true",
		"https://api.example.com/mcp/, true",
		"HTTPS://API.example.com/mcp, true",
		"https://api.example.com/other, false",
		"https://api.example.com, false",
		"https://evil.example.com/mcp, false",
		"'', false"
	})
	void testIsAudience(String audience, boolean expected) {
		// GIVEN
		sut = ProtectedResources.of(ISSUER, List.of("/mcp"), List.of());

		// WHEN
		boolean result = sut.isAudience(audience);

		// THEN
		assertThat(result).isEqualTo(expected);
	}
}
