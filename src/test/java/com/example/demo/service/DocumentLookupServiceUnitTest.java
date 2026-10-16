package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.response.DecolectaDniResponse;
import com.example.demo.dto.response.DecolectaRucResponse;
import com.example.demo.dto.response.DniLookupResponse;
import com.example.demo.dto.response.RucLookupResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.feignclient.DecolectaDniClient;
import com.example.demo.feignclient.DecolectaRucClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentLookupServiceUnitTest {

    @Test
    void dniLookupValidatesAndMapsReniecData() {
        DecolectaDniClient client = mock(DecolectaDniClient.class);
        DniLookupService service = new DniLookupService(client);
        ReflectionTestUtils.setField(service, "token", "token");
        when(client.findByDni("12345678", "Bearer token"))
                .thenReturn(new DecolectaDniResponse("12345678", " Ana ", " Perez ", " Diaz "));

        assertThat(service.lookup("12345678")).isEqualTo(new DniLookupResponse("12345678", "Ana", "Perez Diaz"));
        assertThatThrownBy(() -> service.lookup("123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El DNI debe tener exactamente 8 digitos.");
    }

    @Test
    void dniLookupReportsAnUnconfiguredProvider() {
        DniLookupService service = new DniLookupService(mock(DecolectaDniClient.class));
        ReflectionTestUtils.setField(service, "token", "");

        assertThatThrownBy(() -> service.lookup("12345678"))
                .isInstanceOf(ApiException.class)
                .hasMessage("La consulta RENIEC no esta configurada.");
    }

    @Test
    void rucLookupValidatesAndMapsSunatData() {
        DecolectaRucClient client = mock(DecolectaRucClient.class);
        RucLookupService service = new RucLookupService(client);
        ReflectionTestUtils.setField(service, "token", "token");
        when(client.findByRuc("20123456789", "Bearer token"))
                .thenReturn(new DecolectaRucResponse(" Empresa SAC ", "20123456789", " Av. Uno "));

        assertThat(service.lookup("20123456789")).isEqualTo(new RucLookupResponse("Empresa SAC", "20123456789", "Av. Uno"));
        assertThatThrownBy(() -> service.lookup("201"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El RUC debe tener exactamente 11 digitos.");
    }
}
