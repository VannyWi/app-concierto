package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.request.CompanyRequest;
import com.example.demo.entity.CompanyEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompanyServiceUnitTest {

    @Test
    void rejectsDuplicateRucBeforeSavingTheCompany() {
        CompanyRepository companies = mock(CompanyRepository.class);
        CompanyDataProtector protector = mock(CompanyDataProtector.class);
        when(protector.rucHash("20123456789")).thenReturn("hash");
        when(companies.findByRucHash("hash")).thenReturn(Optional.of(new CompanyEntity()));
        CompanyService service = new CompanyService(companies, mock(UserRepository.class), protector);

        assertThatThrownBy(() -> service.create(new CompanyRequest("Empresa", "20123456789", null), "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El RUC ya se encuentra registrado.");
    }
}
