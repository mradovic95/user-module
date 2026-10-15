package com.comex.usermodule.authorizationserver;

import java.util.UUID;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers the application's own client ({@code first-party-client-id}, e.g. its frontend): a public client allowed
 * the {@code password} grant and rotated refresh tokens, with the configured token lifetimes. Runs once every
 * singleton exists, so the JDBC repository's tables are already migrated; the registration is updated in place when
 * the settings change.
 */
@Slf4j
@RequiredArgsConstructor
public class FirstPartyClientRegistrar implements SmartInitializingSingleton {

	private final RegisteredClientRepository registeredClientRepository;
	private final AuthorizationServerProperties properties;

	@Override
	public void afterSingletonsInstantiated() {
		String clientId = properties.getFirstPartyClientId();
		if (!StringUtils.hasText(clientId)) {
			return;
		}
		RegisteredClient existing = registeredClientRepository.findByClientId(clientId);
		registeredClientRepository.save(firstPartyClient(existing == null ? UUID.randomUUID().toString()
			: existing.getId(), clientId));
		log.info("{} first-party client {}.", existing == null ? "Registered" : "Updated", clientId);
	}

	private RegisteredClient firstPartyClient(String id, String clientId) {
		return RegisteredClient.withId(id)
			.clientId(clientId)
			.clientName(clientId + " (first-party)")
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.scopes(scopes -> scopes.addAll(properties.getScopesSupported()))
			.clientSettings(ClientSettings.builder().requireProofKey(false).requireAuthorizationConsent(false).build())
			.tokenSettings(TokenSettings.builder()
				.accessTokenTimeToLive(properties.getAccessTokenTtl())
				.refreshTokenTimeToLive(properties.getRefreshTokenTtl())
				.reuseRefreshTokens(false)
				.build())
			.build();
	}
}
