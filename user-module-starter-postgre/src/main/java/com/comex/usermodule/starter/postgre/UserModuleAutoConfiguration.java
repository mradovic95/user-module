package com.comex.usermodule.starter.postgre;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.comex.usermodule.configuration.SecurityConfiguration;
import com.comex.usermodule.configuration.UserConfiguration;
import com.comex.usermodule.configuration.UserProperties;
import com.comex.usermodule.starter.postgre.configuration.UserPostgreRepositoryConfiguration;

@AutoConfiguration
@EnableConfigurationProperties(UserProperties.class)
@Import({UserConfiguration.class, UserPostgreRepositoryConfiguration.class, SecurityConfiguration.class})
public class UserModuleAutoConfiguration {
}
