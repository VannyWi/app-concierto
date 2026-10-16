package com.example.demo.dto.request;

import com.example.demo.entity.Role;
import com.example.demo.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "debe tener exactamente 8 digitos") String dni,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank
        @ValidPassword
        String password,
        @NotNull Role role) {
}
