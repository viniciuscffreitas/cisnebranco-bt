package com.cisnebranco.repository;

import com.cisnebranco.entity.AvailabilityWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityWindowRepository extends JpaRepository<AvailabilityWindow, Long> {

    List<AvailabilityWindow> findByGroomerIdAndActiveTrue(Long groomerId);

    List<AvailabilityWindow> findByGroomerIdAndDayOfWeekAndActiveTrue(Long groomerId, Integer dayOfWeek);
}
