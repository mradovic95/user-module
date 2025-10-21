package com.comex.usermodule.infrastructure.persistence.dynamodb;

import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.dynamodb.entity.RoleDynamoEntity;
import com.comex.usermodule.infrastructure.persistence.dynamodb.mapper.UserDynamoEntityMapper;
import com.comex.usermodule.infrastructure.persistence.dynamodb.repository.UserDynamoRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.comex.usermodule.infrastructure.persistence.dynamodb.AbstractDynamoDbIntegrationTest.localstack;

@TestConfiguration
public class DynamoDbTestConfiguration {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public Map<String, RoleDynamoEntity> roleDynamoCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public UserDynamoEntityMapper userDynamoEntityMapper() {
        return new UserDynamoEntityMapper();
    }

    @Bean
    public UserRepository userRepository(
            DynamoDbEnhancedClient enhancedClient,
            UserDynamoEntityMapper mapper,
            Map<String, RoleDynamoEntity> roleDynamoCache) {
        return new UserDynamoRepository(enhancedClient, "users", mapper, roleDynamoCache);
    }
}
