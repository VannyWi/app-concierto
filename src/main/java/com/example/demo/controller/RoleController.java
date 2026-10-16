package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.RoleResponse;
import com.example.demo.repository.RoleRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    public ApiResponse<List<RoleResponse>> findAll() {
        List<RoleResponse> roles = roleRepository.findAll().stream()
                .map(role -> new RoleResponse(role.getId(), role.getRoleName()))
                .sorted(Comparator.comparing(RoleResponse::roleName))
                .toList();
        return new ApiResponse<>(true, "Roles retrieved", roles);
    }
}
