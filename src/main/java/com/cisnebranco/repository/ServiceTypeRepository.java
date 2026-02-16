package com.cisnebranco.repository;

import com.cisnebranco.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    Optional<ServiceType> findByCode(String code);

    List<ServiceType> findByActiveTrue();

    Optional<ServiceType> findByIdAndActiveTrue(Long id);
}
