package com.comex.usermodule.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "user")
public class UserProperties {

	private JwtProperties jwt = new JwtProperties();
	private boolean verificationRequired = false;
	private PersistenceProperties persistence = new PersistenceProperties();
	private DynamoDbProperties dynamodb = new DynamoDbProperties();
	private OAuth2Properties oauth2 = new OAuth2Properties();

	@Data
	public static class JwtProperties {

		private String jwtSecretKey = "simpleSecretKeyasdadadadadadadadasdadasdasdasd";
		private Long jwtExpiration = 3600L * 1000L;
	}

	@Data
	public static class PersistenceProperties {

		private String type = "postgresql";
	}

	@Data
	public static class DynamoDbProperties {

		private String tableName = "users";
		private String region = "us-east-1";
		private String endpoint;
	}

	@Data
	public static class OAuth2Properties {

		/**
		 * Where to send the browser after a successful OAuth2 (Google) login. The module JWT is appended as a
		 * {@code token} query parameter. When blank, the JWT is written to the response body as JSON instead.
		 */
		private String successRedirectUrl;

		/**
		 * Email domains (the part after {@code @}) allowed to sign in with Google, e.g. {@code comex.com}. Matching
		 * is case-insensitive and exact. Empty means every Google account is accepted.
		 */
		private List<String> allowedDomains = new ArrayList<>();
	}
}
