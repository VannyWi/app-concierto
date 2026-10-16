package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CompanyDataProtectorUnitTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsWithoutExposingPlaintext() {
        CompanyDataProtector protector = new CompanyDataProtector(KEY);
        String encrypted = protector.encrypt("Empresa privada");

        assertThat(encrypted).isNotEqualTo("Empresa privada");
        assertThat(protector.decrypt(encrypted)).isEqualTo("Empresa privada");
        assertThat(protector.rucHash("20123456789")).isEqualTo(protector.rucHash("20123456789"));
    }

    @Test
    void rejectsInvalidKeysAndPayloads() {
        assertThatThrownBy(() -> new CompanyDataProtector("invalid"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CompanyDataProtector(KEY).decrypt("invalid"))
                .isInstanceOf(IllegalStateException.class);
    }
}
