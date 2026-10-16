package com.example.demo.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record CompanyRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Pattern(regexp = "\\d{11}") String ruc, @Size(max = 250) String address) { }
