package com.cisnebranco.repository;

import com.cisnebranco.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByClientId(Long clientId);

    Page<Pet> findByActiveTrue(Pageable pageable);

    List<Pet> findByClientIdAndActiveTrue(Long clientId);

    Optional<Pet> findByIdAndActiveTrue(Long id);
}
