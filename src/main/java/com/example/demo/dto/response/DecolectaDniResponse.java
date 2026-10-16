package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DecolectaDniResponse(
        @JsonProperty("document_number") String documentNumber,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("first_last_name") String firstLastName,
        @JsonProperty("second_last_name") String secondLastName) {
}
