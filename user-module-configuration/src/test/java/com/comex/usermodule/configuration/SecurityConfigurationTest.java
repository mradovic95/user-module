package com.comex.usermodule.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.security.OAuth2LoginSuccessHandler;
import com.comex.usermodule.security.jwt.JwtAuthFilter;

class SecurityConfigurationTest {

	private static final String GOOGLE_CLIENT_ID = "spring.security.oauth2.client.registration.google.client-id=test-client-id";
	private static final String GOOGLE_CLIENT_SECRET = "spring.security.oauth2.client.registration.google.client-secret=test-client-secret";

	private final WebApplicationContextRunner sut = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			ServletWebSecurityAutoConfiguration.class,
			OAuth2ClientAutoConfiguration.class,
			OAuth2ClientWebSecurityAutoConfiguration.class,
			UserConfiguration.class,
			SecurityConfiguration.class,
			OAuth2GoogleConfiguration.class))
		.withUserConfiguration(PropertiesConfiguration.class)
		.withBean(UserRepository.class, () -> mock(UserRepository.class));

	@Test
	void testFilterChainWithoutOAuth2ClientRegistration() {
		// GIVEN
		// no spring.security.oauth2.client.* properties

		// WHEN
		sut.run(context -> {
			// THEN
			assertThat(context).hasSingleBean(SecurityFilterChain.class);
			assertThat(context).hasBean("userModuleSecurityFilterChain");
			SecurityFilterChain chain = context.getBean(SecurityFilterChain.class);
			assertThat(chain.getFilters()).anyMatch(JwtAuthFilter.class::isInstance);
			assertThat(chain.getFilters()).noneMatch(OAuth2LoginAuthenticationFilter.class::isInstance);
		});
	}

	@Test
	void testFilterChainWithOAuth2ClientRegistration() {
		// GIVEN
		WebApplicationContextRunner runner = sut.withPropertyValues(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET,
			"user.oauth2.success-redirect-url=https://app.example.com/callback",
			"user.oauth2.allowed-domains=comex.com,leanpay.com");

		// WHEN
		runner.run(context -> {
			// THEN
			assertThat(context).hasSingleBean(SecurityFilterChain.class);
			assertThat(context).hasSingleBean(OAuth2LoginSuccessHandler.class);
			UserProperties.OAuth2Properties oauth2 = context.getBean(UserProperties.class).getOauth2();
			assertThat(oauth2.getSuccessRedirectUrl()).isEqualTo("https://app.example.com/callback");
			assertThat(oauth2.getAllowedDomains()).containsExactly("comex.com", "leanpay.com");
			SecurityFilterChain chain = context.getBean(SecurityFilterChain.class);
			assertThat(chain.getFilters()).anyMatch(JwtAuthFilter.class::isInstance);
			assertThat(chain.getFilters()).anyMatch(OAuth2LoginAuthenticationFilter.class::isInstance);
		});
	}

	@Test
	void testFilterChainBacksOffWhenApplicationDefinesItsOwn() {
		// GIVEN
		WebApplicationContextRunner runner = sut.withUserConfiguration(CustomSecurityFilterChainConfiguration.class);

		// WHEN
		runner.run(context -> {
			// THEN
			assertThat(context).hasSingleBean(SecurityFilterChain.class);
			assertThat(context).hasBean("customSecurityFilterChain");
			assertThat(context).doesNotHaveBean("userModuleSecurityFilterChain");
			assertThat(context.getBean(SecurityFilterChain.class).getFilters())
				.noneMatch(JwtAuthFilter.class::isInstance);
		});
	}

	@Test
	void testPublicAndProtectedEndpoints() {
		// GIVEN
		WebApplicationContextRunner runner = sut.withPropertyValues(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET);

		// WHEN
		runner.run(context -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
				.apply(springSecurity())
				.build();

			// THEN
			// public endpoints pass security (404 because no controller is registered in this slice)
			mockMvc.perform(post("/user")).andExpect(status().isNotFound());
			mockMvc.perform(post("/user/login")).andExpect(status().isNotFound());
			mockMvc.perform(get("/user/verify").param("code", "x")).andExpect(status().isNotFound());
			// OAuth2 entry point redirects to Google
			mockMvc.perform(get("/oauth2/authorization/google"))
				.andExpect(status().is3xxRedirection())
				.andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
					.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
			// protected endpoint without a token gets the JSON 401
			mockMvc.perform(get("/user").param("email", "a@b.c"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized - Token is missing or invalid"));
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(UserProperties.class)
	static class PropertiesConfiguration {
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSecurityFilterChainConfiguration {

		@Bean
		SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
			return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
		}
	}
}
