package com.comex.usermodule.infrastructure.persistence.jpa;


import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.comex.usermodule.infrastructure.persistence.entity.UserEntity;

public interface UserPostgreJpaRepository extends JpaRepository<UserEntity, Long> {

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserEntity> findByEmailAndStatus(String email, String status);

	Optional<UserEntity> findByVerificationCode(String verificationCode);
}
