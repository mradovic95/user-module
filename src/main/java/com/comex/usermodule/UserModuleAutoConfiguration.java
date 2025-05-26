package com.comex.usermodule;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.comex.usermodule.configuration.OAuth2GoogleConfiguration;
import com.comex.usermodule.configuration.SecurityConfiguration;
import com.comex.usermodule.configuration.UserConfiguration;
import com.comex.usermodule.configuration.UserProperties;

@AutoConfiguration
@EnableConfigurationProperties(UserProperties.class)
@Import({UserConfiguration.class, SecurityConfiguration.class, OAuth2GoogleConfiguration.class})
public class UserModuleAutoConfiguration {
}
