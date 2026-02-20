package com.cisnebranco.repository;

import com.cisnebranco.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.groomer.id = :groomerId
              AND a.status NOT IN (com.cisnebranco.entity.enums.AppointmentStatus.CANCELLED,
                                   com.cisnebranco.entity.enums.AppointmentStatus.NO_SHOW)
              AND a.scheduledStart < :end
              AND a.scheduledEnd > :start
            """)
    List<Appointment> findActiveByGroomerAndDateRange(@Param("groomerId") Long groomerId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.client
            JOIN FETCH a.pet p LEFT JOIN FETCH p.breed
            JOIN FETCH a.groomer JOIN FETCH a.serviceType
            LEFT JOIN FETCH a.technicalOs
            WHERE a.scheduledStart >= :start AND a.scheduledStart < :end
            ORDER BY a.scheduledStart
            """)
    List<Appointment> findByDateRange(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.client
            JOIN FETCH a.pet p LEFT JOIN FETCH p.breed
            JOIN FETCH a.groomer JOIN FETCH a.serviceType
            LEFT JOIN FETCH a.technicalOs
            WHERE a.client.id = :clientId
            ORDER BY a.scheduledStart DESC
            """)
    List<Appointment> findByClientId(@Param("clientId") Long clientId);
}
