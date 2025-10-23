package com.comex.usermodule.starter.postgre.configuration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.comex.usermodule.configuration.UserProperties;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.RoleDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.mapper.UserDynamoEntityMapper;
import com.comex.usermodule.infrastructure.persistence.dynamodb.repository.UserDynamoRepository;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@AutoConfiguration
@ConditionalOnProperty(name = "user.persistence.type", havingValue = "dynamodb")
@Slf4j
public class UserDynamoRepositoryConfiguration {

	@Autowired
	private UserProperties userProperties;

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbClient dynamoDbClient() {
		log.info("Configuring DynamoDB client for region: {}", userProperties.getDynamodb().getRegion());

		DynamoDbClientBuilder builder = DynamoDbClient.builder()
			.region(Region.of(userProperties.getDynamodb().getRegion()))
			.credentialsProvider(DefaultCredentialsProvider.create());

		// If endpoint is specified (for local testing), use it
		if (userProperties.getDynamodb().getEndpoint() != null &&
			!userProperties.getDynamodb().getEndpoint().isEmpty()) {
			log.info("Using custom DynamoDB endpoint: {}", userProperties.getDynamodb().getEndpoint());
			builder.endpointOverride(URI.create(userProperties.getDynamodb().getEndpoint()));
		}

		return builder.build();
	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
		return DynamoDbEnhancedClient.builder()
			.dynamoDbClient(dynamoDbClient)
			.build();
	}

	@ConditionalOnMissingBean
	@Bean
	public UserDynamoEntityMapper userDynamoEntityMapper() {
		return new UserDynamoEntityMapper();
	}

	@ConditionalOnMissingBean
	@Bean
	public Map<String, RoleDynamoEntity> roleDynamoCache() {
		return new HashMap<>();
	}

	@ConditionalOnMissingBean
	@Bean
	public UserRepository userRepository(DynamoDbEnhancedClient enhancedClient,
		UserDynamoEntityMapper userDynamoEntityMapper, Map<String, RoleDynamoEntity> roleDynamoCache) {

		log.info("Configuring DynamoDB UserRepository with table: {}",
			userProperties.getDynamodb().getTableName());

		return new UserDynamoRepository(
			enhancedClient,
			userProperties.getDynamodb().getTableName(),
			userDynamoEntityMapper,
			roleDynamoCache
		);
	}
}
