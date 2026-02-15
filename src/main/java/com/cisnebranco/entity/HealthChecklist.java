package com.cisnebranco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "health_checklists")
@Getter
@Setter
@NoArgsConstructor
public class HealthChecklist extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_os_id", nullable = false, unique = true)
    private TechnicalOs technicalOs;

    @Column(name = "skin_condition")
    private String skinCondition;

    @Column(name = "coat_condition")
    private String coatCondition;

    @Column(name = "has_fleas", nullable = false)
    private boolean hasFleas;

    @Column(name = "has_ticks", nullable = false)
    private boolean hasTicks;

    @Column(name = "has_wounds", nullable = false)
    private boolean hasWounds;

    @Column(name = "ear_condition")
    private String earCondition;

    @Column(name = "nail_condition")
    private String nailCondition;

    private String observations;
}
