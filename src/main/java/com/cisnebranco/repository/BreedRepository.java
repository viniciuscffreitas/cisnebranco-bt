package com.cisnebranco.repository;

import com.cisnebranco.entity.Breed;
import com.cisnebranco.entity.enums.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findBySpecies(Species species);
}
