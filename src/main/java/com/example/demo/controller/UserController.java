package com.example.demo.controller;

import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.request.UpdateUserRequest;
import com.example.demo.dto.request.UpdateUserStatusRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.DniLookupResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import com.example.demo.service.DniLookupService;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class UserController {

    private final UserService userService;
    private final DniLookupService dniLookupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody RegisterRequest request) {
        return new ApiResponse<>(true, "User created", userService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return new ApiResponse<>(true, "Users retrieved", userService.findAll(firstName, lastName, roleId, active, page));
    }

    @GetMapping("/dni/{dni}")
    public ApiResponse<DniLookupResponse> lookupDni(@PathVariable String dni) {
        return new ApiResponse<>(true, "DNI consultado", dniLookupService.lookup(dni));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "User retrieved", userService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return new ApiResponse<>(true, "User updated", userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable UUID id, @RequestBody UpdateUserStatusRequest request) {
        return new ApiResponse<>(true, "User status updated", userService.updateStatus(id, request.active()));
    }

}
