package com.example.demo.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtCookieServiceUnitTest {

    @Test
    void writesSecureHttpOnlyCookieAndClearsIt() {
        JwtCookieService service = new JwtCookieService(Duration.ofHours(8), true);
        HttpServletResponse response = mock(HttpServletResponse.class);

        service.addTokenCookie(response, "token");
        service.clearTokenCookie(response);

        verify(response).addHeader(eq("Set-Cookie"), org.mockito.ArgumentMatchers.contains("ACCESS_TOKEN=token"));
        verify(response).addHeader(eq("Set-Cookie"), org.mockito.ArgumentMatchers.contains("Max-Age=0"));
    }
}
