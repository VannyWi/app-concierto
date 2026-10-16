package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ApplicationSettingsResponse;
import com.example.demo.service.ApplicationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ApplicationSettingsController {
    private final ApplicationSettingsService settings;

    @GetMapping("/api/v1/public/settings")
    public ApiResponse<ApplicationSettingsResponse> current() {
        return new ApiResponse<>(true, "Configuración obtenida.", settings.current());
    }

    @GetMapping("/api/v1/public/settings/logo")
    public ResponseEntity<byte[]> logo() {
        ApplicationSettingsService.Logo logo = settings.logo();
        return logo == null ? ResponseEntity.notFound().build()
                : ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.contentType())).body(logo.content());
    }

    @PutMapping(value = "/api/v1/settings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ApplicationSettingsResponse> update(
            @RequestParam String applicationName,
            @RequestParam(required = false) MultipartFile logo) {
        return new ApiResponse<>(true, "Configuración actualizada.", settings.update(applicationName, logo));
    }
}
