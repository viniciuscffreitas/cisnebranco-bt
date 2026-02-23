package com.cisnebranco.entity;

import com.cisnebranco.entity.enums.IncidentCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident_reports")
@Getter @Setter @NoArgsConstructor
public class IncidentReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_os_id", nullable = false)
    private TechnicalOs technicalOs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "reported_by", length = 100)
    private String reportedBy;

    @OneToMany(mappedBy = "incidentReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentMedia> media = new ArrayList<>();
}
