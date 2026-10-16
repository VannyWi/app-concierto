package com.example.demo.service;

import com.example.demo.dto.request.ProjectRequest;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProjectResponse;
import com.example.demo.dto.response.ProjectStateHistoryResponse;
import com.example.demo.dto.response.ProjectDashboardResponse;
import com.example.demo.dto.response.ProjectDashboardYearResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.ProjectStateHistoryRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ProjectService {
    private static final int PAGE_SIZE = 20;
    private final ProjectRepository projects;
    private final CompanyRepository companies;
    private final ProjectStateHistoryRepository history;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final CompanyDataProtector protector;

    public PageResponse<ProjectResponse> findAll(String search, YearMonth eventMonth, ProjectApproval approved, ProjectStatus status, int page) {
        Specification<ProjectEntity> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventMonth != null) predicates.add(cb.between(root.get("eventDate"), eventMonth.atDay(1), eventMonth.atEndOfMonth()));
            if (approved != null) predicates.add(cb.equal(root.get("approved"), approved));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        List<ProjectEntity> filtered = projects.findAll(filter, Sort.by(Sort.Direction.DESC, "eventDate")).stream().filter(project -> matchesSearch(project, search)).toList();
        int from = Math.min(page * PAGE_SIZE, filtered.size());
        int to = Math.min(from + PAGE_SIZE, filtered.size());
        return new PageResponse<>(filtered.subList(from, to).stream().map(this::toResponse).toList(), page, PAGE_SIZE, filtered.size(), (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
    }

    public ProjectResponse findById(UUID id) {
        return toResponse(projects.findById(id).orElseThrow(() -> ApiException.notFound("Proyecto no encontrado.")));
    }

    @Transactional(readOnly = true)
    public List<ProjectStateHistoryResponse> history(UUID id) {
        projects.findById(id).orElseThrow(() -> ApiException.notFound("Proyecto no encontrado."));
        return history.findByProjectIdOrderByChangedAtDesc(id).stream().map(item -> new ProjectStateHistoryResponse(
                item.getId(), item.getChangedBy().getFirstName() + " " + item.getChangedBy().getLastName(), item.getPreviousApproval().name(),
                item.getNewApproval().name(), item.getPreviousStatus().name(), item.getNewStatus().name(), item.getChangedAt())).toList();
    }

    public boolean hasProjects(int fromYear, int toYear) {
        validateYearRange(fromYear, toYear);
        return projects.existsByYearBetween(fromYear, toYear);
    }

    public List<Integer> years() {
        return projects.findDistinctYears();
    }

    public List<com.example.demo.dto.response.ProjectExecutiveResponse> executives() {
        return projects.findDistinctExecutives();
    }

    public ProjectDashboardResponse dashboard(int fromYear, int toYear, UUID companyId, UUID executiveId) {
        validateYearRange(fromYear, toYear);
        int years = toYear - fromYear + 1;
        long[] executed = new long[years];
        long[] lost = new long[years];
        long[] cancelled = new long[years];
        long pending = 0;
        List<ProjectEntity> dashboardProjects = projects.findByYearBetween(fromYear, toYear).stream()
                .filter(project -> companyId == null || companyId.equals(project.getCompany().getId()))
                .filter(project -> executiveId == null || executiveId.equals(project.getCreatedBy().getId()))
                .toList();
        for (ProjectEntity project : dashboardProjects) {
            if (project.getApproved() == ProjectApproval.PENDIENTE) pending++;
            int year = project.getYear() - fromYear;
            switch (project.getStatus()) {
                case EJECUTADO -> executed[year]++;
                case PERDIDO -> lost[year]++;
                case CANCELADO -> cancelled[year]++;
                default -> { }
            }
        }
        List<ProjectDashboardYearResponse> yearly = java.util.stream.IntStream.range(0, years)
                .mapToObj(year -> new ProjectDashboardYearResponse(fromYear + year, executed[year], lost[year], cancelled[year]))
                .toList();
        return new ProjectDashboardResponse(
                pending,
                java.util.Arrays.stream(executed).sum(),
                java.util.Arrays.stream(lost).sum(),
                java.util.Arrays.stream(cancelled).sum(),
                yearly);
    }

    @Transactional(readOnly = true)
    public void export(int fromYear, int toYear, OutputStream output) throws IOException {
        validateYearRange(fromYear, toYear);
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Stream<ProjectEntity> projectStream = projects.streamByYearRange(fromYear, toYear);
        try (workbook; projectStream) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet("Proyectos " + fromYear + "-" + toYear);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            CellStyle percentageStyle = workbook.createCellStyle();
            percentageStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            String[] headers = { "EJECUTIVO", "EMPRESA", "PROYECTO", "OBSERVACIONES", "APROBADO (SI/NO)", "ESTADO", "PPTO TOTAL (sin IGV)", "AÑO", "FECHA EVENTO", "FACTURACIÓN (mes tentativo)", "FACTURACION FINAL (Sin IGV)", "% RENTABILIDAD" };
            Row header = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
            int[] widths = { 22, 28, 30, 42, 19, 16, 21, 10, 16, 30, 27, 18 };
            for (int column = 0; column < widths.length; column++) sheet.setColumnWidth(column, widths[column] * 256);

            final int[] rowNumber = { 1 };
            projectStream.forEach(project -> {
                Row row = sheet.createRow(rowNumber[0]++);
                textCell(row, 0, project.getCreatedBy().getFirstName() + " " + project.getCreatedBy().getLastName());
                textCell(row, 1, protector.decrypt(project.getCompany()).name());
                textCell(row, 2, project.getProjectName());
                textCell(row, 3, project.getObservations());
                textCell(row, 4, project.getApproved().name());
                textCell(row, 5, project.getStatus().name());
                numberCell(row, 6, project.getTotalBudget(), amountStyle);
                row.createCell(7).setCellValue(project.getYear());
                Cell eventDate = row.createCell(8);
                eventDate.setCellValue(java.sql.Date.valueOf(project.getEventDate()));
                eventDate.setCellStyle(dateStyle);
                textCell(row, 9, project.getTentativeBillingMonth());
                numberCell(row, 10, project.getFinalBilling(), amountStyle);
                BigDecimal projectProfitability = profitability(project);
                if (projectProfitability != null) {
                    Cell profitability = row.createCell(11);
                    profitability.setCellValue(projectProfitability.divide(BigDecimal.valueOf(100)).doubleValue());
                    profitability.setCellStyle(percentageStyle);
                }
                // Keep the persistence context bounded while the database cursor advances.
                if (rowNumber[0] % 100 == 0) entityManager.clear();
            });
            workbook.write(output);
        } finally {
            workbook.dispose();
        }
    }

    @Transactional public ProjectResponse create(ProjectRequest request, String username) {
        UserEntity actor = actor(username);
        ProjectEntity project = new ProjectEntity();
        project.setCreatedBy(actor);
        project.setApproved(ProjectApproval.PENDIENTE);
        project.setStatus(ProjectStatus.VIGENTE);
        project.setExecutiveLocked(false);
        copy(project, request, true);
        return toResponse(projects.save(project));
    }

    @Transactional public ProjectResponse update(UUID id, ProjectRequest request, String username) {
        UserEntity actor = actor(username);
        ProjectEntity project = projects.findById(id).orElseThrow(() -> ApiException.notFound("Proyecto no encontrado"));
        if (!admin(actor) && !project.getCreatedBy().getId().equals(actor.getId())) {
            throw ApiException.forbidden("No tienes permiso para modificar este proyecto");
        }
        if (!admin(actor) && project.getStatus() != ProjectStatus.VIGENTE) {
            throw ApiException.forbidden("Los ejecutivos no pueden editar proyectos finalizados");
        }
        ProjectApproval nextApproval = request.approved() == null ? project.getApproved() : request.approved();
        ProjectStatus nextStatus = request.status() == null ? project.getStatus() : request.status();
        validateStateTransition(project.getApproved(), project.getStatus(), nextApproval, nextStatus);
        boolean approvalChanged = project.getApproved() != nextApproval;
        boolean statusChanged = project.getStatus() != nextStatus;
        if (approvalChanged || statusChanged) {
            recordStateChange(project, actor, project.getApproved(), nextApproval, project.getStatus(), nextStatus, Instant.now());
        }
        project.setApproved(nextApproval); project.setStatus(nextStatus);
        copy(project, request, false);
        return toResponse(project);
    }

    private void copy(ProjectEntity project, ProjectRequest request, boolean isCreating) {
        CompanyEntity company = companies.findById(request.companyId()).orElseThrow(() -> ApiException.notFound("Empresa no encontrada"));
        if (isCreating && !company.isActive()) throw ApiException.badRequest("Las empresas inactivas no pueden crear proyectos");
        project.setCompany(company);
        project.setProjectName(request.projectName().trim()); project.setObservations(request.observations() == null || request.observations().isBlank() ? null : request.observations().trim());
        project.setTotalBudget(request.totalBudget()); project.setYear(request.year()); project.setEventDate(request.eventDate()); project.setTentativeBillingMonth(request.tentativeBillingMonth());
        project.setFinalBilling(request.finalBilling()); project.setCostsWithoutVat(request.costsWithoutVat());
    }

    private void validateState(ProjectApproval approval, ProjectStatus status) {
        boolean valid = approval == ProjectApproval.PENDIENTE && status == ProjectStatus.VIGENTE
                || approval == ProjectApproval.SI && (status == ProjectStatus.VIGENTE || status == ProjectStatus.EJECUTADO)
                || approval == ProjectApproval.NO && (status == ProjectStatus.VIGENTE || status == ProjectStatus.PERDIDO || status == ProjectStatus.CANCELADO);
        if (!valid) throw ApiException.badRequest("La combinación de aprobación y estado no es válida");
    }
    private void validateStateTransition(ProjectApproval previousApproval, ProjectStatus previousStatus, ProjectApproval nextApproval, ProjectStatus nextStatus) {
        validateState(nextApproval, nextStatus);
        if (previousApproval == ProjectApproval.PENDIENTE) {
            if (nextApproval == ProjectApproval.SI && nextStatus != ProjectStatus.VIGENTE) throw ApiException.badRequest("La aprobación debe completarse antes de ejecutar un proyecto");
            if (nextApproval == ProjectApproval.NO && nextStatus != ProjectStatus.VIGENTE) throw ApiException.badRequest("La no aprobación debe confirmarse antes de definir el estado final");
            return;
        }
        if (previousApproval != nextApproval) throw ApiException.badRequest("La aprobación no puede cambiarse después de decidirse");
        boolean validStatusTransition = previousStatus == ProjectStatus.VIGENTE
                && (previousApproval == ProjectApproval.SI && nextStatus == ProjectStatus.EJECUTADO
                || previousApproval == ProjectApproval.NO && (nextStatus == ProjectStatus.PERDIDO || nextStatus == ProjectStatus.CANCELADO));
        if (previousStatus != nextStatus && !validStatusTransition) {
            throw ApiException.badRequest("El estado de un proyecto no puede revertirse");
        }
    }
    private void recordStateChange(ProjectEntity project, UserEntity actor, ProjectApproval previousApproval, ProjectApproval nextApproval, ProjectStatus previousStatus, ProjectStatus nextStatus, Instant changedAt) {
        ProjectStateHistoryEntity entry = new ProjectStateHistoryEntity();
        entry.setProject(project); entry.setChangedBy(actor); entry.setPreviousApproval(previousApproval); entry.setNewApproval(nextApproval);
        entry.setPreviousStatus(previousStatus); entry.setNewStatus(nextStatus); entry.setChangedAt(changedAt);
        history.save(entry);
    }
    private UserEntity actor(String username) { return users.findByUsername(UserService.normalizeUsername(username)).orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación.")); }
    private boolean admin(UserEntity user) { return Role.ADMIN.name().equals(user.getRole().getRoleName()); }
    private boolean matchesSearch(ProjectEntity project, String search) {
        if (search == null || search.isBlank()) return true;
        String value = search.trim().toLowerCase(Locale.ROOT);
        String executive = (project.getCreatedBy().getFirstName() + " " + project.getCreatedBy().getLastName()).toLowerCase(Locale.ROOT);
        return executive.contains(value) || project.getProjectName().toLowerCase(Locale.ROOT).contains(value) || protector.decrypt(project.getCompany()).name().toLowerCase(Locale.ROOT).contains(value);
    }
    private void validateYearRange(int fromYear, int toYear) {
        if (toYear < fromYear) throw ApiException.badRequest("El año final no puede ser anterior al año inicial.");
        if (toYear - fromYear >= 5) throw ApiException.badRequest("El rango de años no puede superar 5 años.");
    }
    private BigDecimal profitability(ProjectEntity project) {
        if (project.getFinalBilling() == null || project.getFinalBilling().signum() == 0) return null;
        return project.getFinalBilling().subtract(project.getTotalBudget()).multiply(BigDecimal.valueOf(100)).divide(project.getFinalBilling(), 2, RoundingMode.HALF_UP);
    }
    private CellStyle headerStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }
    private void textCell(Row row, int column, String value) {
        if (value != null) row.createCell(column).setCellValue(value);
    }
    private void numberCell(Row row, int column, BigDecimal value, CellStyle style) {
        if (value == null) return;
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }
    private ProjectResponse toResponse(ProjectEntity project) {
        CompanyEntity company = project.getCompany(); UserEntity creator = project.getCreatedBy();
        CompanyDataProtector.CompanyData companyData = protector.decrypt(company);
        return new ProjectResponse(project.getId(), creator.getId(), creator.getFirstName() + " " + creator.getLastName(), company.getId(), companyData.name(), companyData.ruc(), project.getProjectName(), project.getObservations(), project.getApproved().name(), project.getStatus().name(), project.getTotalBudget(), project.getYear(), project.getEventDate(), project.getTentativeBillingMonth(), project.getFinalBilling(), project.getCostsWithoutVat(), profitability(project), project.isExecutiveLocked());
    }
}
