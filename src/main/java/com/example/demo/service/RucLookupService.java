package com.example.demo.service;

import com.example.demo.dto.response.DecolectaRucResponse;
import com.example.demo.dto.response.RucLookupResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.feignclient.DecolectaRucClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RucLookupService {

    private final DecolectaRucClient decolectaRucClient;

    @Value("${app.decolecta.token:}")
    private String token;

    public RucLookupResponse lookup(String ruc) {
        if (!ruc.matches("\\d{11}")) {
            throw ApiException.badRequest("El RUC debe tener exactamente 11 digitos.");
        }
        if (token.isBlank()) {
            throw ApiException.serviceUnavailable("La consulta SUNAT no esta configurada.");
        }

        try {
            DecolectaRucResponse response = decolectaRucClient.findByRuc(ruc, "Bearer " + token);
            if (response == null || response.ruc() == null || response.ruc().isBlank()) {
                throw ApiException.notFound("No se encontro informacion para el RUC indicado.");
            }
            return new RucLookupResponse(trim(response.name()), response.ruc(), trim(response.address()));
        } catch (FeignException exception) {
            if (exception.status() == 404 || exception.status() == 422) {
                throw ApiException.notFound("No se encontro informacion para el RUC indicado.");
            }
            throw ApiException.serviceUnavailable("No se pudo consultar SUNAT en este momento.");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
