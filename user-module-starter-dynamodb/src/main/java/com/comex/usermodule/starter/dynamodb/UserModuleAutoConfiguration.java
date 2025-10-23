package com.comex.usermodule.starter.dynamodb	;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.comex.usermodule.configuration.SecurityConfiguration;
import com.comex.usermodule.configuration.UserConfiguration;
import com.comex.usermodule.configuration.UserProperties;
import com.comex.usermodule.starter.postgre.configuration.UserDynamoRepositoryConfiguration;

@AutoConfiguration
@EnableConfigurationProperties(UserProperties.class)
@Import({UserConfiguration.class, UserDynamoRepositoryConfiguration.class, SecurityConfiguration.class})
public class UserModuleAutoConfiguration {
}
