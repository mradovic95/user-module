package com.comex.usermodule.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Matches when at least one {@code spring.security.oauth2.client.registration.*} entry is configured, i.e. the same
 * trigger Spring Boot uses to create a {@code ClientRegistrationRepository}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnOAuth2ClientRegistrationCondition.class)
public @interface ConditionalOnOAuth2ClientRegistration {
}
