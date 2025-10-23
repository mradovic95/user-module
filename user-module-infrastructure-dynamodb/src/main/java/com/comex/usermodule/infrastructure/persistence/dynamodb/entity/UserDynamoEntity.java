package com.comex.usermodule.infrastructure.persistence.dynamodb.entity;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@DynamoDbBean
public class UserDynamoEntity {

	private String id;
	private String username;
	private String password;
	private String email;
	private Instant createdAt;
	private String status;
	private String verificationCode;
	private List<RoleDynamoEntity> roles;

	@DynamoDbPartitionKey
	@DynamoDbAttribute("email")
	public String getEmail() {
		return email;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = "verification-code-index")
	@DynamoDbAttribute("verificationCode")
	public String getVerificationCode() {
		return verificationCode;
	}

	@DynamoDbAttribute("id")
	public String getId() {
		return id;
	}

	@DynamoDbAttribute("username")
	public String getUsername() {
		return username;
	}

	@DynamoDbAttribute("password")
	public String getPassword() {
		return password;
	}

	@DynamoDbAttribute("createdAt")
	public Instant getCreatedAt() {
		return createdAt;
	}

	@DynamoDbAttribute("status")
	public String getStatus() {
		return status;
	}

	@DynamoDbAttribute("roles")
	public List<RoleDynamoEntity> getRoles() {
		return roles;
	}
}
