package com.example.demo.controller;

import com.example.demo.dto.request.ProjectRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProjectResponse;
import com.example.demo.dto.response.ProjectStateHistoryResponse;
import com.example.demo.dto.response.ProjectDashboardResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.entity.ProjectApproval;
import com.example.demo.entity.ProjectStatus;
import com.example.demo.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<PageResponse<ProjectResponse>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth eventMonth,
            @RequestParam(required = false) ProjectApproval approved,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return new ApiResponse<>(true, "Projects retrieved", projectService.findAll(search, eventMonth, approved, status, page));
    }

    @GetMapping("/export")
    public void export(
            @RequestParam @Min(2000) @Max(2100) int fromYear,
            @RequestParam @Min(2000) @Max(2100) int toYear,
            HttpServletResponse response) throws IOException {
        if (!projectService.hasProjects(fromYear, toYear)) throw ApiException.badRequest("No hay proyectos para exportar.");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("proyectos-" + fromYear + "-" + toYear + ".xlsx", StandardCharsets.UTF_8)
                .build().toString());
        projectService.export(fromYear, toYear, response.getOutputStream());
    }

    @GetMapping("/years")
    public ApiResponse<java.util.List<Integer>> years() {
        return new ApiResponse<>(true, "Project years retrieved", projectService.years());
    }

    @GetMapping("/executives")
    public ApiResponse<java.util.List<com.example.demo.dto.response.ProjectExecutiveResponse>> executives() {
        return new ApiResponse<>(true, "Project executives retrieved", projectService.executives());
    }

    @GetMapping("/dashboard")
    public ApiResponse<ProjectDashboardResponse> dashboard(
            @RequestParam @Min(2000) @Max(2100) int fromYear,
            @RequestParam @Min(2000) @Max(2100) int toYear,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID executiveId) {
        return new ApiResponse<>(true, "Project dashboard retrieved", projectService.dashboard(fromYear, toYear, companyId, executiveId));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<java.util.List<ProjectStateHistoryResponse>> history(@PathVariable UUID id) {
        return new ApiResponse<>(true, "Project history retrieved", projectService.history(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> findById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "Project retrieved", projectService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectRequest request, Authentication authentication) {
        return new ApiResponse<>(true, "Project created", projectService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')")
    public ApiResponse<ProjectResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ProjectRequest request, Authentication authentication) {
        return new ApiResponse<>(true, "Project updated", projectService.update(id, request, authentication.getName()));
    }
}
