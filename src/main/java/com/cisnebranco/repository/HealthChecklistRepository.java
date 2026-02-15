package com.cisnebranco.repository;

import com.cisnebranco.entity.HealthChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthChecklistRepository extends JpaRepository<HealthChecklist, Long> {
    Optional<HealthChecklist> findByTechnicalOsId(Long technicalOsId);
}
