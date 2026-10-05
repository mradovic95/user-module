package com.comex.usermodule.authorizationserver;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

/**
 * Turns a Dynamic Client Registration request into a {@link RegisteredClient} with the module's policy applied on
 * top of Spring's defaults: PKCE required, consent per configuration, configured token lifetimes, rotated refresh
 * tokens, the {@code refresh_token} grant always available and the advertised scopes granted to the client.
 */
public final class UserModuleRegisteredClientConverter implements Converter<OAuth2ClientRegistration, RegisteredClient> {

	private final OAuth2ClientRegistrationRegisteredClientConverter delegate = new OAuth2ClientRegistrationRegisteredClientConverter();
	private final AuthorizationServerProperties properties;

	public UserModuleRegisteredClientConverter(AuthorizationServerProperties properties) {
		this.properties = properties;
		this.delegate.setTokenSettingsCustomizer(tokenSettings -> tokenSettings
			.accessTokenTimeToLive(properties.getAccessTokenTtl())
			.refreshTokenTimeToLive(properties.getRefreshTokenTtl())
			.reuseRefreshTokens(false));
	}

	@Override
	public RegisteredClient convert(OAuth2ClientRegistration clientRegistration) {
		RegisteredClient registeredClient = delegate.convert(clientRegistration);
		ClientSettings clientSettings = ClientSettings.withSettings(registeredClient.getClientSettings().getSettings())
			.requireProofKey(true)
			.requireAuthorizationConsent(properties.isConsentRequired())
			.build();
		return RegisteredClient.from(registeredClient)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.scopes(scopes -> scopes.addAll(properties.getScopesSupported()))
			.clientSettings(clientSettings)
			.build();
	}
}
