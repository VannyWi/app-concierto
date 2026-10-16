package com.example.demo.service;

import com.example.demo.dto.response.ApplicationSettingsResponse;
import com.example.demo.entity.ApplicationSettingsEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ApplicationSettingsRepository;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ApplicationSettingsService {
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final ApplicationSettingsRepository settings;

    @Transactional
    public ApplicationSettingsResponse current() {
        return response(settings());
    }

    @Transactional(readOnly = true)
    public Logo logo() {
        ApplicationSettingsEntity setting = settings.findById(1L).orElse(null);
        return setting == null || setting.getLogo() == null ? null : new Logo(setting.getLogo(), setting.getLogoContentType());
    }

    @Transactional
    public ApplicationSettingsResponse update(String applicationName, MultipartFile logo) {
        if (applicationName == null || applicationName.isBlank()) throw ApiException.badRequest("Ingresa el nombre de la aplicación.");
        ApplicationSettingsEntity setting = settings();
        setting.setApplicationName(applicationName.trim());
        if (logo != null && !logo.isEmpty()) setLogo(setting, logo);
        return response(setting);
    }

    private ApplicationSettingsEntity settings() {
        return settings.findById(1L).orElseGet(() -> settings.save(new ApplicationSettingsEntity()));
    }

    private void setLogo(ApplicationSettingsEntity setting, MultipartFile logo) {
        if (logo.getSize() > MAX_LOGO_SIZE) throw ApiException.badRequest("El logo no puede superar los 2 MB.");
        String contentType = logo.getContentType();
        if (!IMAGE_TYPES.contains(contentType)) throw ApiException.badRequest("El logo debe ser una imagen PNG, JPEG o WebP.");
        try {
            setting.setLogo(logo.getBytes());
            setting.setLogoContentType(contentType);
        } catch (IOException exception) {
            throw ApiException.badRequest("No fue posible leer el logo.");
        }
    }

    private ApplicationSettingsResponse response(ApplicationSettingsEntity setting) {
        return new ApplicationSettingsResponse(setting.getApplicationName(), setting.getLogo() != null);
    }

    public record Logo(byte[] content, String contentType) { }
}
