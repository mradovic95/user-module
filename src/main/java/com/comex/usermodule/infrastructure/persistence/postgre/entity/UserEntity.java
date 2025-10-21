package com.comex.usermodule.infrastructure.persistence.postgre.entity;

import java.time.Instant;
import java.util.Set;

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
@Table(name = "user_table")
@Entity
public class UserEntity {

	@SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
	@Id
	private Long id;
	@Column(nullable = false)
	private String username;
	@Column
	private String password;
	@Column(nullable = false)
	private String email;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@Column(nullable = false)
	private String status;
	@Column(name = "verification_code")
	private String verificationCode;
	@JoinTable(
		name = "users_roles",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	@Fetch(FetchMode.JOIN)
	@ManyToMany(fetch = FetchType.EAGER)
	private Set<RoleEntity> roles;
}
