package com.cisnebranco.repository;

import com.cisnebranco.entity.ServiceTypeBreedPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceTypeBreedPriceRepository extends JpaRepository<ServiceTypeBreedPrice, Long> {

    @Query("""
            SELECT p FROM ServiceTypeBreedPrice p
            JOIN FETCH p.serviceType st
            JOIN FETCH p.breed
            WHERE p.breed.id = :breedId
              AND st.active = true
            ORDER BY st.name
            """)
    List<ServiceTypeBreedPrice> findByBreedId(@Param("breedId") Long breedId);
}
