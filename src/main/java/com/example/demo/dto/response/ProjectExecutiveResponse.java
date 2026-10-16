package com.example.demo.dto.response;

import java.util.UUID;

public record ProjectExecutiveResponse(UUID id, String firstName, String lastName, String username) {
}
