package com.comex.usermodule.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "user")
public class UserProperties {

	private JwtProperties jwt = new JwtProperties();
	private boolean verificationRequired = false;
	private PersistenceProperties persistence = new PersistenceProperties();
	private DynamoDbProperties dynamodb = new DynamoDbProperties();

	@Data
	public static class JwtProperties {

		private String jwtSecretKey = "simpleSecretKeyasdadadadadadadadasdadasdasdasd";
		private Long jwtExpiration = 3600L * 1000L;
	}

	@Data
	public static class PersistenceProperties {

		private String type = "postgresql"; // postgresql or dynamodb
	}

	@Data
	public static class DynamoDbProperties {

		private String tableName = "users";
		private String region = "us-east-1";
		private String endpoint; // optional, for local testing
	}
}
