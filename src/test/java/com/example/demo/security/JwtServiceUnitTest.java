package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceUnitTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void createsAndParsesASignedToken() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        UserEntity user = user("admin");
        UUID sessionId = UUID.randomUUID();

        assertThat(service.parseToken(service.createToken(user, sessionId)))
                .contains(new JwtService.JwtPayload("admin", sessionId));
    }

    @Test
    void rejectsExpiredAndMalformedTokens() {
        JwtService expired = new JwtService(SECRET, Duration.ofSeconds(-1));
        assertThat(expired.parseToken(expired.createToken(user("admin"), UUID.randomUUID()))).isEmpty();
        assertThat(expired.parseToken("not-a-token")).isEmpty();
    }

    @Test
    void requiresA256BitSigningKey() {
        assertThatThrownBy(() -> new JwtService(Base64.getEncoder().encodeToString(new byte[31]), Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private UserEntity user(String username) {
        RoleEntity role = new RoleEntity();
        role.setRoleName("ADMIN");
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
