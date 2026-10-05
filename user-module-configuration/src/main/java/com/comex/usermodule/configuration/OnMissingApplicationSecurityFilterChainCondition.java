package com.comex.usermodule.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Condition behind {@link ConditionalOnMissingApplicationSecurityFilterChain}: no {@link SecurityFilterChain} bean
 * is registered except the ones owned by this module.
 */
class OnMissingApplicationSecurityFilterChainCondition extends SpringBootCondition implements ConfigurationCondition {

	/** Every {@link SecurityFilterChain} bean defined by the module uses this bean-name prefix. */
	static final String MODULE_BEAN_NAME_PREFIX = "userModule";

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
			.forCondition(ConditionalOnMissingApplicationSecurityFilterChain.class);
		List<String> applicationChains = applicationSecurityFilterChains(context.getBeanFactory());
		if (applicationChains.isEmpty()) {
			return ConditionOutcome.match(message.didNotFind("application-defined SecurityFilterChain bean").atAll());
		}
		return ConditionOutcome.noMatch(message.found("application-defined SecurityFilterChain bean",
			"application-defined SecurityFilterChain beans").items(applicationChains));
	}

	private static List<String> applicationSecurityFilterChains(ConfigurableListableBeanFactory beanFactory) {
		if (beanFactory == null) {
			return List.of();
		}
		return Arrays.stream(beanFactory.getBeanNamesForType(SecurityFilterChain.class, true, false))
			.filter(name -> !name.startsWith(MODULE_BEAN_NAME_PREFIX))
			.toList();
	}
}
