package com.comex.usermodule.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "user")
public class UserProperties {

	private JwtProperties jwt = new JwtProperties();
	private boolean verificationRequired = false;

	@Data
	public static class JwtProperties {

		private String jwtSecretKey = "simpleSecretKeyasdadadadadadadadasdadasdasdasd";
		private Long jwtExpiration = 3600L * 1000L;
	}
}
