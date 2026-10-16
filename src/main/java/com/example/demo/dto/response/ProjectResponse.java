package com.example.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID createdById,
        String executiveName,
        UUID companyId,
        String companyName,
        String companyRuc,
        String projectName,
        String observations,
        String approved,
        String status,
        BigDecimal totalBudget,
        int year,
        LocalDate eventDate,
        String tentativeBillingMonth,
        BigDecimal finalBilling,
        BigDecimal costsWithoutVat,
        BigDecimal profitability,
        boolean executiveLocked) {
}
