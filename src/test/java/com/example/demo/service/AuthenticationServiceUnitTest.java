package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

class AuthenticationServiceUnitTest {

    @Test
    void createsASessionAndTokenForValidCredentials() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        UserRepository users = mock(UserRepository.class);
        UserService userService = mock(UserService.class);
        JwtService jwt = mock(JwtService.class);
        UserEntity user = user();
        when(users.findByUsernameForUpdate("admin")).thenReturn(Optional.of(user));
        when(manager.authenticate(any())).thenReturn(null);
        when(jwt.createToken(eq(user), any())).thenReturn("token");
        when(userService.toResponse(user)).thenReturn(new UserResponse(user.getId(), "admin", null, "Admin", "User", "ADMIN", true));

        var result = new AuthenticationService(manager, users, userService, jwt).login(new LoginRequest(" ADMIN ", "Password123!"));

        assertThat(result.token()).isEqualTo("token");
        assertThat(user.getActiveSessionId()).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void rejectsInactiveUsersBeforeAuthentication() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        UserRepository users = mock(UserRepository.class);
        UserEntity user = user();
        user.setActive(false);
        when(users.findByUsernameForUpdate("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> new AuthenticationService(manager, users, mock(UserService.class), mock(JwtService.class))
                .login(new LoginRequest("admin", "Password123!")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Credenciales incorrectas.");
    }

    private UserEntity user() {
        RoleEntity role = new RoleEntity();
        role.setRoleName("ADMIN");
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setRole(role);
        return user;
    }
}
