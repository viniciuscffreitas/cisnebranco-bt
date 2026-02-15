package com.cisnebranco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_commissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"groomer_id", "week_start"})
})
@Getter
@Setter
@NoArgsConstructor
public class WeeklyCommission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groomer_id", nullable = false)
    private Groomer groomer;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "total_services")
    private int totalServices;

    @Column(name = "total_revenue", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;
}
