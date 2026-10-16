package com.example.demo.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordValidatorUnitTest {

    @Test
    void acceptsOnlyPasswordsWithAllRequiredCharacterTypes() {
        assertThat(PasswordValidator.isValidPassword("Password123!")).isTrue();
        assertThat(PasswordValidator.isValidPassword("password123")).isFalse();
        assertThat(PasswordValidator.isValidPassword("Password!")).isFalse();
        assertThat(PasswordValidator.isValidPassword(null)).isTrue();
    }
}
