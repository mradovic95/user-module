package com.comex.usermodule.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Matches when the application has not defined a {@code SecurityFilterChain} bean of its own.
 * <p>
 * Unlike {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}, the module's own chains (bean names starting
 * with {@value OnMissingApplicationSecurityFilterChainCondition#MODULE_BEAN_NAME_PREFIX}, e.g. the Google login chain
 * or the authorization server chain) are ignored, so the order in which the module's configurations are processed
 * cannot accidentally switch the default API chain off.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMissingApplicationSecurityFilterChainCondition.class)
public @interface ConditionalOnMissingApplicationSecurityFilterChain {
}
