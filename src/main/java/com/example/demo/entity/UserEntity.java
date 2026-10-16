package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    private static final int MAXIMUM_FAILED_LOGIN_ATTEMPTS = 3;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "active_session_id")
    private UUID activeSessionId;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(nullable = false, length = 20)
    private String status = ACTIVE_STATUS;

    public boolean isActive() {
        return ACTIVE_STATUS.equals(status);
    }

    public void setActive(boolean active) {
        status = active ? ACTIVE_STATUS : INACTIVE_STATUS;
        if (!active) {
            activeSessionId = null;
        }
    }

    public void recordFailedLoginAttempt() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAXIMUM_FAILED_LOGIN_ATTEMPTS) {
            status = INACTIVE_STATUS;
            activeSessionId = null;
        }
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
    }
}
