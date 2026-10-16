package com.example.demo.service;

import com.example.demo.dto.response.DecolectaDniResponse;
import com.example.demo.dto.response.DniLookupResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.feignclient.DecolectaDniClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DniLookupService {

    private final DecolectaDniClient decolectaDniClient;

    @Value("${app.decolecta.token:}")
    private String token;

    public DniLookupResponse lookup(String dni) {
        if (!dni.matches("\\d{8}")) {
            throw ApiException.badRequest("El DNI debe tener exactamente 8 digitos.");
        }
        if (token.isBlank()) {
            throw ApiException.serviceUnavailable("La consulta RENIEC no esta configurada.");
        }

        DecolectaDniResponse response;
        try {
            response = decolectaDniClient.findByDni(dni, "Bearer " + token);
        } catch (FeignException exception) {
            if (exception.status() == 404 || exception.status() == 422) {
                throw ApiException.notFound("No se encontro informacion para el DNI indicado.");
            }
            throw ApiException.serviceUnavailable("No se pudo consultar RENIEC en este momento.");
        } catch (RuntimeException exception) {
            throw ApiException.serviceUnavailable("No se pudo consultar RENIEC en este momento.");
        }

        if (response == null || response.documentNumber() == null || response.documentNumber().isBlank()) {
            throw ApiException.notFound("No se encontro informacion para el DNI indicado.");
        }
        return new DniLookupResponse(
                response.documentNumber().trim(),
                trim(response.firstName()),
                joinLastNames(response.firstLastName(), response.secondLastName()));
    }

    private String joinLastNames(String firstLastName, String secondLastName) {
        return String.join(" ", trim(firstLastName), trim(secondLastName)).trim();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
