package com.example.demo.dto.response;

import java.util.UUID;

public record UserResponse(UUID id, String username, String dni, String firstName, String lastName, String roleName, boolean active) {
}
