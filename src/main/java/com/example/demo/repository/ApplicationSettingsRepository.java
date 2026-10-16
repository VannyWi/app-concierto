package com.example.demo.repository;

import com.example.demo.entity.ApplicationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettingsEntity, Long> {
}
