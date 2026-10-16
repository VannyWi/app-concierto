package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.ApplicationSettingsEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ApplicationSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class ApplicationSettingsServiceUnitTest {

    @Test
    void updatesNameAndValidLogo() throws Exception {
        ApplicationSettingsRepository repository = mock(ApplicationSettingsRepository.class);
        ApplicationSettingsEntity settings = new ApplicationSettingsEntity();
        MultipartFile logo = mock(MultipartFile.class);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(logo.isEmpty()).thenReturn(false);
        when(logo.getSize()).thenReturn(3L);
        when(logo.getContentType()).thenReturn("image/png");
        when(logo.getBytes()).thenReturn(new byte[] {1, 2, 3});

        var response = new ApplicationSettingsService(repository).update(" Eventos ", logo);

        assertThat(response.applicationName()).isEqualTo("Eventos");
        assertThat(response.hasLogo()).isTrue();
    }

    @Test
    void rejectsBlankNamesAndUnsupportedLogos() {
        ApplicationSettingsRepository repository = mock(ApplicationSettingsRepository.class);
        ApplicationSettingsService service = new ApplicationSettingsService(repository);
        MultipartFile logo = mock(MultipartFile.class);
        when(repository.findById(1L)).thenReturn(Optional.of(new ApplicationSettingsEntity()));
        when(logo.isEmpty()).thenReturn(false);
        when(logo.getSize()).thenReturn(1L);
        when(logo.getContentType()).thenReturn("text/plain");

        assertThatThrownBy(() -> service.update(" ", null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.update("Conciertos", logo)).isInstanceOf(ApiException.class);
    }
}
