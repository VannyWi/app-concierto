package com.example.demo.repository;

import com.example.demo.entity.ProjectStateHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStateHistoryRepository extends JpaRepository<ProjectStateHistoryEntity, UUID> { List<ProjectStateHistoryEntity> findByProjectIdOrderByChangedAtDesc(UUID projectId); }
