package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "executive_id", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "project_name", nullable = false, length = 150)
    private String projectName;

    @Column(length = 2000)
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProjectApproval approved;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ProjectStatus status;

    @Column(name = "executive_locked", nullable = false)
    private boolean executiveLocked;

    @Column(name = "total_budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "event_year", nullable = false)
    private int year;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "tentative_billing_month", nullable = false, length = 7)
    private String tentativeBillingMonth;

    @Column(name = "final_billing", precision = 15, scale = 2)
    private BigDecimal finalBilling;

    @Column(name = "costs_without_vat", precision = 15, scale = 2)
    private BigDecimal costsWithoutVat;
}
