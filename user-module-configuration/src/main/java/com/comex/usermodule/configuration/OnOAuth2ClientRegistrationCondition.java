package com.comex.usermodule.configuration;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition behind {@link ConditionalOnOAuth2ClientRegistration}. Mirrors Spring Boot's package-private
 * {@code ClientsConfiguredCondition}: binds {@code spring.security.oauth2.client.registration} and matches when the
 * map is not empty.
 */
class OnOAuth2ClientRegistrationCondition extends SpringBootCondition {

	static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	private static final Bindable<Map<String, OAuth2ClientProperties.Registration>> REGISTRATIONS = Bindable
		.mapOf(String.class, OAuth2ClientProperties.Registration.class);

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnOAuth2ClientRegistration.class);
		Map<String, OAuth2ClientProperties.Registration> registrations = Binder.get(context.getEnvironment())
			.bind(REGISTRATION_PREFIX, REGISTRATIONS)
			.orElse(Map.of());
		if (registrations.isEmpty()) {
			return ConditionOutcome.noMatch(message.notAvailable("registered OAuth2 clients"));
		}
		return ConditionOutcome.match(message.foundExactly("registered OAuth2 clients " + registrations.keySet()));
	}
}
