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

import java.math.BigDecimal;

@Entity
@Table(name = "os_service_items")
@Getter
@Setter
@NoArgsConstructor
public class OsServiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_os_id", nullable = false)
    private TechnicalOs technicalOs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @Column(name = "locked_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal lockedPrice;

    @Column(name = "locked_commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal lockedCommissionRate;

    @Column(name = "commission_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionValue;
}
