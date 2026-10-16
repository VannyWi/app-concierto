package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DecolectaRucResponse(
        @JsonProperty("razon_social") String name,
        @JsonProperty("numero_documento") String ruc,
        @JsonProperty("direccion") String address) {
}
