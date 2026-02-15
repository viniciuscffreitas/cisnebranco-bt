package com.cisnebranco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inspection_photos")
@Getter
@Setter
@NoArgsConstructor
public class InspectionPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_os_id", nullable = false)
    private TechnicalOs technicalOs;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    private String caption;
}
