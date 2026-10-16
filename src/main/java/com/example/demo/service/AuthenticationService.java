package com.example.demo.service;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(LoginRequest request) {
        String username = UserService.normalizeUsername(request.username());
        UserEntity user = userRepository.findByUsernameForUpdate(username).orElse(null);
        if (user != null && !user.isActive()) {
            throw ApiException.unauthorized("Credenciales incorrectas.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            if (user != null) {
                user.recordFailedLoginAttempt();
            }
            throw ApiException.unauthorized("Credenciales incorrectas.");
        }

        if (user == null) {
            throw ApiException.unauthorized("Credenciales incorrectas.");
        }

        user.resetFailedLoginAttempts();
        UUID sessionId = UUID.randomUUID();
        user.setActiveSessionId(sessionId);
        return new LoginResult(jwtService.createToken(user, sessionId), new AuthResponse(userService.toResponse(user)));
    }

    @Transactional
    public void logout(String token) {
        jwtService.parseToken(token).ifPresent(payload -> userRepository.findByUsernameForUpdate(payload.username())
                .filter(user -> payload.sessionId().equals(user.getActiveSessionId()))
                .ifPresent(user -> user.setActiveSessionId(null)));
    }

    public String refreshToken(String username) {
        UserEntity user = userRepository.findByUsername(UserService.normalizeUsername(username))
                .orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación."));
        if (user.getActiveSessionId() == null) throw ApiException.unauthorized("Se requiere autenticación.");
        return jwtService.createToken(user, user.getActiveSessionId());
    }

    public record LoginResult(String token, AuthResponse response) {
    }
}
