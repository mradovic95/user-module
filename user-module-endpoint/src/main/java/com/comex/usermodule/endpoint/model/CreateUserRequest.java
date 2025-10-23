package com.comex.usermodule.endpoint.model;

public record CreateUserRequest(String username, String password, String email) {
}
