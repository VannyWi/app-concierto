package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.UpdateAccountRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.security.JwtCookieService;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtCookieService jwtCookieService;
    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationService.LoginResult login = authenticationService.login(request);
        jwtCookieService.addTokenCookie(response, login.token());
        return new ApiResponse<>(true, "Login successful", login.response());
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser(Authentication authentication) {
        return new ApiResponse<>(true, "Authenticated user", userService.currentUser(authentication.getName()));
    }

    @PutMapping("/account")
    public ApiResponse<UserResponse> updateAccount(
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication,
            HttpServletResponse response) {
        UserResponse user = userService.updateCurrentAccount(authentication.getName(), request);
        jwtCookieService.addTokenCookie(response, authenticationService.refreshToken(user.username()));
        return new ApiResponse<>(true, "Cuenta actualizada.", user);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = JwtCookieService.COOKIE_NAME, required = false) String token,
            HttpServletResponse response) {
        if (token != null) {
            authenticationService.logout(token);
        }
        jwtCookieService.clearTokenCookie(response);
        return new ApiResponse<>(true, "Logout successful", null);
    }
}
