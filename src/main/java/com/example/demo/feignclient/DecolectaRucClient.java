package com.example.demo.feignclient;

import com.example.demo.dto.response.DecolectaRucResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "decolecta-ruc-client", url = "https://api.decolecta.com/v1/sunat/ruc")
public interface DecolectaRucClient {

    @GetMapping
    DecolectaRucResponse findByRuc(
            @RequestParam("numero") String ruc,
            @RequestHeader("Authorization") String authorization);
}
