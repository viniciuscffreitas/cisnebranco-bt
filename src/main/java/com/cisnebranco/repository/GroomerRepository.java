package com.cisnebranco.repository;

import com.cisnebranco.entity.Groomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroomerRepository extends JpaRepository<Groomer, Long> {
    List<Groomer> findByActiveTrue();
}
