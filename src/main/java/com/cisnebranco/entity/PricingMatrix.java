package com.cisnebranco.entity;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_matrix", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"service_type_id", "species", "pet_size"})
})
@Getter
@Setter
@NoArgsConstructor
public class PricingMatrix extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_size", nullable = false)
    private PetSize petSize;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
