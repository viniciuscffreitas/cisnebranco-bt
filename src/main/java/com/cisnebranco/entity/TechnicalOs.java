package com.cisnebranco.entity;

import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "technical_os")
@Getter
@Setter
@NoArgsConstructor
public class TechnicalOs extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groomer_id")
    private Groomer groomer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OsStatus status = OsStatus.SCHEDULED;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "total_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    // Generated column in DB; read-only in Java
    @Column(name = "balance", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, updatable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "total_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "payment_balance", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal paymentBalance;

    @OneToMany(mappedBy = "technicalOs")
    private List<PaymentEvent> paymentEvents = new ArrayList<>();

    @OneToMany(mappedBy = "technicalOs", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsServiceItem> serviceItems = new ArrayList<>();

    @OneToMany(mappedBy = "technicalOs", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InspectionPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "technicalOs")
    private List<IncidentReport> incidentReports = new ArrayList<>();

    @OneToOne(mappedBy = "technicalOs", cascade = CascadeType.ALL, orphanRemoval = true)
    private HealthChecklist healthChecklist;

    @OneToOne(mappedBy = "technicalOs", fetch = FetchType.LAZY)
    private Appointment appointment;

    public BigDecimal getBalance() {
        if (balance != null) return balance;
        return totalPrice.subtract(totalCommission);
    }
}
