package com.example.demo.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtCookieService {

    public static final String COOKIE_NAME = "ACCESS_TOKEN";

    private final Duration expiration;
    private final boolean secureCookie;

    public JwtCookieService(
            @Value("${app.jwt.expiration}") Duration expiration,
            @Value("${app.jwt.secure-cookie}") boolean secureCookie) {
        this.expiration = expiration;
        this.secureCookie = secureCookie;
    }

    public void addTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, expiration).toString());
    }

    public void clearTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
