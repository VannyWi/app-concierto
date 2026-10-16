package com.example.demo.dto.request;

import com.example.demo.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "debe tener exactamente 8 digitos") String dni,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @ValidPassword
        String password,
        UUID roleId,
        boolean active) {
}
