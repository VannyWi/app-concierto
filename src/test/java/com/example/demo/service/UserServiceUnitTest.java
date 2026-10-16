package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.request.UpdateUserRequest;
import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceUnitTest {

    @Test
    void administratorCannotReuseTheUsersCurrentPassword() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserEntity user = user("Password123!");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.findByUsername("user")).thenReturn(Optional.of(user));
        when(users.findByDni("12345678")).thenReturn(Optional.of(user));
        when(roles.findById(user.getRole().getId())).thenReturn(Optional.of(user.getRole()));
        UserService service = new UserService(users, roles, new BCryptPasswordEncoder());

        UpdateUserRequest request = new UpdateUserRequest("user", "12345678", "User", "Test", "Password123!", user.getRole().getId(), true);

        assertThatThrownBy(() -> service.update(user.getId(), request))
                .isInstanceOf(ApiException.class)
                .hasMessage("La nueva contraseña no puede ser igual a la actual.");
    }

    @Test
    void passwordChangeRotatesTheActiveSession() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserEntity user = user("Password123!");
        UUID previousSession = UUID.randomUUID();
        user.setActiveSessionId(previousSession);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.findByUsername("user")).thenReturn(Optional.of(user));
        when(users.findByDni("12345678")).thenReturn(Optional.of(user));
        when(roles.findById(user.getRole().getId())).thenReturn(Optional.of(user.getRole()));
        UserService service = new UserService(users, roles, new BCryptPasswordEncoder());

        service.update(user.getId(), new UpdateUserRequest("user", "12345678", "User", "Test", "ChangedPassword1!", user.getRole().getId(), true));

        assertThat(user.getActiveSessionId()).isNotEqualTo(previousSession);
    }

    private UserEntity user(String password) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setRoleName("EJECUTIVO");
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("user");
        user.setDni("12345678");
        user.setFirstName("User");
        user.setLastName("Test");
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setRole(role);
        return user;
    }
}
