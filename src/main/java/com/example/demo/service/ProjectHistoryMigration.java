package com.example.demo.service;

import com.example.demo.entity.ProjectApproval;
import com.example.demo.entity.ProjectStateHistoryEntity;
import com.example.demo.entity.ProjectStatus;
import com.example.demo.repository.ProjectStateHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(110)
@RequiredArgsConstructor
public class ProjectHistoryMigration implements ApplicationRunner {

    private final ProjectStateHistoryRepository history;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ProjectStateHistoryEntity entry : history.findAll()) {
            boolean combinedRejection = entry.getPreviousApproval() == ProjectApproval.PENDIENTE
                    && entry.getNewApproval() == ProjectApproval.NO
                    && entry.getPreviousStatus() == ProjectStatus.VIGENTE
                    && (entry.getNewStatus() == ProjectStatus.PERDIDO || entry.getNewStatus() == ProjectStatus.CANCELADO);
            if (!combinedRejection) continue;

            ProjectStateHistoryEntity finalState = new ProjectStateHistoryEntity();
            finalState.setProject(entry.getProject());
            finalState.setChangedBy(entry.getChangedBy());
            finalState.setPreviousApproval(ProjectApproval.NO);
            finalState.setNewApproval(ProjectApproval.NO);
            finalState.setPreviousStatus(entry.getPreviousStatus());
            finalState.setNewStatus(entry.getNewStatus());
            finalState.setChangedAt(entry.getChangedAt().plusMillis(1));
            history.save(finalState);

            entry.setNewStatus(entry.getPreviousStatus());
        }
    }
}
