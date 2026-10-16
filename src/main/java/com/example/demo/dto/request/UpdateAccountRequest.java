package com.example.demo.dto.request;

import com.example.demo.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank String currentPassword,
        @ValidPassword String newPassword) {
}
