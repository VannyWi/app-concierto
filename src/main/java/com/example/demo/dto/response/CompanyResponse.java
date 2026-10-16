package com.example.demo.dto.response;
import java.util.UUID;
public record CompanyResponse(UUID id, String name, String ruc, String address, boolean active) { }
