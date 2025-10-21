package com.comex.usermodule.infrastructure.persistence.jpa;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comex.usermodule.infrastructure.persistence.entity.RoleEntity;


public interface RolePostgreJpaRepository extends JpaRepository<RoleEntity, Long> {

	Set<RoleEntity> findAllByNameIn(Set<String> name);
}
