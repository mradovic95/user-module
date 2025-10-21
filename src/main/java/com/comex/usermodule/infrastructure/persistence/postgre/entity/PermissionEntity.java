package com.comex.usermodule.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "permission")
@Entity
public class PermissionEntity {

	@SequenceGenerator(name = "permission_id_seq", sequenceName = "permission_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permission_id_seq")
	@Id
	private Long id;
	@Column(nullable = false)
	private String name;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
