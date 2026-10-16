package com.example.demo.service;

import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.request.UpdateUserRequest;
import com.example.demo.dto.request.UpdateAccountRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.example.demo.dto.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> findAll(
            String firstName, String lastName, UUID roleId, Boolean active, int page) {
        Specification<UserEntity> filters = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (firstName != null && !firstName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")), "%" + firstName.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (lastName != null && !lastName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")), "%" + lastName.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (roleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("role").get("id"), roleId));
            }
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), active ? "ACTIVE" : "INACTIVE"));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        Page<UserResponse> users = userRepository.findAll(
                        filters, PageRequest.of(page, 20, Sort.by("username").ascending()))
                .map(this::toResponse);
        return new PageResponse<>(
                users.getContent(), users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }

    public UserResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public UserResponse create(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("El usuario ya se encuentra registrado.");
        }
        String dni = normalizeDni(request.dni());
        ensureDniIsAvailable(dni, null);

        RoleEntity role = roleRepository.findByRoleName(request.role().name())
                .orElseThrow(() -> ApiException.notFound("Rol no encontrado."));
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDni(dni);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserEntity user = findEntityById(id);
        String username = normalizeUsername(request.username());
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.conflict("El usuario ya se encuentra registrado.");
        });
        String dni = normalizeDni(request.dni());
        ensureDniIsAvailable(dni, id);
        user.setUsername(username);
        user.setDni(dni);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setActive(request.active());
        if (request.password() != null) {
            rejectCurrentPassword(request.password(), user);
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setActiveSessionId(UUID.randomUUID());
        }
        if (request.roleId() != null) {
            user.setRole(roleRepository.findById(request.roleId())
                    .orElseThrow(() -> ApiException.notFound("Rol no encontrado.")));
        }
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, boolean active) {
        UserEntity user = findEntityById(id);
        user.setActive(active);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentAccount(String currentUsername, UpdateAccountRequest request) {
        UserEntity user = userRepository.findByUsernameForUpdate(normalizeUsername(currentUsername))
                .orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("La contraseña actual no es correcta.");
        }
        String username = normalizeUsername(request.username());
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw ApiException.conflict("El usuario ya se encuentra registrado."); });
        user.setUsername(username);
        if (request.newPassword() != null) {
            rejectCurrentPassword(request.newPassword(), user);
            user.setPassword(passwordEncoder.encode(request.newPassword()));
            user.setActiveSessionId(UUID.randomUUID());
        }
        return toResponse(user);
    }

    public UserResponse currentUser(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
                .map(this::toResponse)
                .orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación."));
    }

    public UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getDni(), user.getFirstName(), user.getLastName(), user.getRole().getRoleName(), user.isActive());
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDni(String dni) {
        return dni == null ? null : dni.trim();
    }

    private void ensureDniIsAvailable(String dni, UUID currentUserId) {
        if (dni == null) return;
        userRepository.findByDni(dni)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("El DNI ya se encuentra registrado.");
                });
    }

    private void rejectCurrentPassword(String password, UserEntity user) {
        if (passwordEncoder.matches(password, user.getPassword())) {
            throw ApiException.badRequest("La nueva contraseña no puede ser igual a la actual.");
        }
    }

    private UserEntity findEntityById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado."));
    }
}
