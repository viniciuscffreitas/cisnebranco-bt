package com.cisnebranco.repository;

import com.cisnebranco.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByClientId(Long clientId);
}
