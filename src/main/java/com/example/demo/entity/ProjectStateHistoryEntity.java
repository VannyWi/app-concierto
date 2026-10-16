package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "project_state_history") @Getter @Setter @NoArgsConstructor
public class ProjectStateHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "changed_by_id", nullable = false) private UserEntity changedBy;
    @Enumerated(EnumType.STRING) @Column(name = "previous_approval", nullable = false, length = 10) private ProjectApproval previousApproval;
    @Enumerated(EnumType.STRING) @Column(name = "new_approval", nullable = false, length = 10) private ProjectApproval newApproval;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false, length = 12) private ProjectStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 12) private ProjectStatus newStatus;
    @Column(nullable = false) private Instant changedAt;
}
