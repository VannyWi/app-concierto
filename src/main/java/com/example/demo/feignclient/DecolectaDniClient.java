package com.example.demo.feignclient;

import com.example.demo.dto.response.DecolectaDniResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "decolecta-dni-client", url = "https://api.decolecta.com/v1/reniec/dni")
public interface DecolectaDniClient {

    @GetMapping
    DecolectaDniResponse findByDni(
            @RequestParam("numero") String dni,
            @RequestHeader("Authorization") String authorization);
}
