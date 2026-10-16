package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.ProjectApproval;
import com.example.demo.entity.ProjectEntity;
import com.example.demo.entity.ProjectStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.ProjectStateHistoryRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectServiceUnitTest {

    @Test
    void calculatesDashboardCountsByYearAndState() {
        ProjectRepository projects = mock(ProjectRepository.class);
        when(projects.findByYearBetween(2025, 2026)).thenReturn(List.of(
                project(2025, ProjectApproval.PENDIENTE, ProjectStatus.VIGENTE),
                project(2025, ProjectApproval.SI, ProjectStatus.EJECUTADO),
                project(2026, ProjectApproval.NO, ProjectStatus.PERDIDO),
                project(2026, ProjectApproval.NO, ProjectStatus.CANCELADO)));
        ProjectService service = service(projects);

        var dashboard = service.dashboard(2025, 2026, null, null);

        assertThat(dashboard.pending()).isEqualTo(1);
        assertThat(dashboard.executed()).isEqualTo(1);
        assertThat(dashboard.lost()).isEqualTo(1);
        assertThat(dashboard.cancelled()).isEqualTo(1);
        assertThat(dashboard.yearly()).hasSize(2);
    }

    @Test
    void rejectsInvalidOrTooLargeYearRanges() {
        ProjectService service = service(mock(ProjectRepository.class));

        assertThatThrownBy(() -> service.hasProjects(2026, 2025)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.hasProjects(2020, 2025)).isInstanceOf(ApiException.class);
    }

    @Test
    void filtersDashboardByExecutive() {
        ProjectRepository projects = mock(ProjectRepository.class);
        UserEntity selectedExecutive = new UserEntity();
        selectedExecutive.setId(java.util.UUID.randomUUID());
        UserEntity otherExecutive = new UserEntity();
        otherExecutive.setId(java.util.UUID.randomUUID());
        ProjectEntity selectedProject = project(2025, ProjectApproval.SI, ProjectStatus.EJECUTADO);
        selectedProject.setCreatedBy(selectedExecutive);
        ProjectEntity otherProject = project(2025, ProjectApproval.NO, ProjectStatus.PERDIDO);
        otherProject.setCreatedBy(otherExecutive);
        when(projects.findByYearBetween(2025, 2025)).thenReturn(List.of(selectedProject, otherProject));

        var dashboard = service(projects).dashboard(2025, 2025, null, selectedExecutive.getId());

        assertThat(dashboard.executed()).isEqualTo(1);
        assertThat(dashboard.lost()).isZero();
    }

    private ProjectService service(ProjectRepository projects) {
        return new ProjectService(projects, mock(CompanyRepository.class), mock(ProjectStateHistoryRepository.class),
                mock(UserRepository.class), mock(EntityManager.class), mock(CompanyDataProtector.class));
    }

    private ProjectEntity project(int year, ProjectApproval approval, ProjectStatus status) {
        ProjectEntity project = new ProjectEntity();
        project.setYear(year);
        project.setApproved(approval);
        project.setStatus(status);
        return project;
    }
}
