package com.example.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.example.demo.entity.ProjectApproval;
import com.example.demo.entity.ProjectStatus;
import java.util.UUID;

public record ProjectRequest(
        @NotNull UUID companyId,
        @NotBlank @Size(max = 150) String projectName,
        @Size(max = 2000) String observations,
        ProjectApproval approved,
        ProjectStatus status,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal totalBudget,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull LocalDate eventDate,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String tentativeBillingMonth,
        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal finalBilling,
        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal costsWithoutVat) {
}
