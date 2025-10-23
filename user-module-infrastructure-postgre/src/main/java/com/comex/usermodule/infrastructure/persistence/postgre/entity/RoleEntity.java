package com.comex.usermodule.infrastructure.persistence.postgre.entity;

import java.time.Instant;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "role")
@Entity
public class RoleEntity {

	@SequenceGenerator(name = "role_id_seq", sequenceName = "role_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_id_seq")
	@Id
	private Long id;
	@Column(nullable = false)
	private String name;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@JoinTable(
		name = "roles_permissions",
		joinColumns = @JoinColumn(name = "role_id"),
		inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	@Fetch(FetchMode.JOIN)
	@BatchSize(size = 100)
	@ManyToMany(fetch = FetchType.EAGER)
	private Set<PermissionEntity> permissions;
}
