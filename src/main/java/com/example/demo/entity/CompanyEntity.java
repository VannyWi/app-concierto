package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "companies") @Getter @Setter @NoArgsConstructor
public class CompanyEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "name_encrypted", length = 1024) private String nameEncrypted;
    @Column(name = "ruc_encrypted", length = 128) private String rucEncrypted;
    @Column(name = "address_encrypted", length = 2048) private String addressEncrypted;
    @Column(name = "ruc_hash", unique = true, length = 64) private String rucHash;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "created_by_id", nullable = false) private UserEntity createdBy;
    public boolean isActive() { return "ACTIVE".equals(status); }
    public void setActive(boolean active) { status = active ? "ACTIVE" : "INACTIVE"; }
}
