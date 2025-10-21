package com.comex.usermodule.infrastructure.persistence.dynamodb.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@DynamoDbBean
public class RoleDynamoEntity {

	private String name;
	private List<PermissionDynamoEntity> permissions;
}
