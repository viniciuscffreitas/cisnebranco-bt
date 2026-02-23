package com.cisnebranco.repository;

import com.cisnebranco.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByTechnicalOsIdOrderByCreatedAtDesc(Long technicalOsId);
}
