package com.example.demo.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "application_settings")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationSettingsEntity {
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 100)
    private String applicationName = "Conciertos";

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] logo;

    @Column(length = 50)
    private String logoContentType;
}
