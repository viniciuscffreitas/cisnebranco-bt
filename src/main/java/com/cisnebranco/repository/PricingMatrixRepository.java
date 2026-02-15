package com.cisnebranco.repository;

import com.cisnebranco.entity.PricingMatrix;
import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingMatrixRepository extends JpaRepository<PricingMatrix, Long> {
    Optional<PricingMatrix> findByServiceTypeIdAndSpeciesAndPetSize(
            Long serviceTypeId, Species species, PetSize petSize);
}
