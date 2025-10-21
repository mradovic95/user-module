package com.comex.usermodule.infrastructure.persistence.postgre;

import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.jpa.RolePostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.jpa.UserPostgreJpaRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.mapper.UserEntityMapper;
import com.comex.usermodule.infrastructure.persistence.postgre.repository.UserPostgreRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class PostgresTestConfiguration {

    @Bean
    public UserEntityMapper userEntityMapper() {
        return new UserEntityMapper();
    }

    @Bean
    public UserRepository userRepository(
            UserPostgreJpaRepository jpaRepository,
            RolePostgreJpaRepository roleJpaRepository,
            UserEntityMapper userEntityMapper) {
        return new UserPostgreRepository(jpaRepository, roleJpaRepository, userEntityMapper);
    }
}
