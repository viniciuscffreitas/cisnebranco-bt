package com.cisnebranco.repository;

import com.cisnebranco.entity.WeeklyCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyCommissionRepository extends JpaRepository<WeeklyCommission, Long> {
    Optional<WeeklyCommission> findByGroomerIdAndWeekStart(Long groomerId, LocalDate weekStart);
    List<WeeklyCommission> findByGroomerId(Long groomerId);
    Page<WeeklyCommission> findByGroomerId(Long groomerId, Pageable pageable);
}
