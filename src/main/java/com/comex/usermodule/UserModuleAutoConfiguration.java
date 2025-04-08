package com.comex.usermodule;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import com.comex.usermodule.configuration.SecurityConfiguration;
import com.comex.usermodule.configuration.UserConfiguration;

@AutoConfiguration
@Import({UserConfiguration.class, SecurityConfiguration.class})
public class UserModuleAutoConfiguration {
}
