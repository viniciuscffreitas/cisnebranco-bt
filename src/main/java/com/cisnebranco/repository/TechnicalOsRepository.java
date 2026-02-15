package com.cisnebranco.repository;

import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.OsStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TechnicalOsRepository extends JpaRepository<TechnicalOs, Long> {

    @EntityGraph(attributePaths = {"pet", "groomer", "serviceItems", "serviceItems.serviceType", "healthChecklist"})
    @Query("SELECT os FROM TechnicalOs os")
    List<TechnicalOs> findAllWithDetails();

    @EntityGraph(attributePaths = {"pet", "groomer", "serviceItems", "serviceItems.serviceType", "healthChecklist"})
    @Query("SELECT os FROM TechnicalOs os WHERE os.groomer.id = :groomerId")
    List<TechnicalOs> findByGroomerIdWithDetails(@Param("groomerId") Long groomerId);

    List<TechnicalOs> findByStatus(OsStatus status);

    @Query("SELECT os FROM TechnicalOs os WHERE os.groomer.id = :groomerId " +
           "AND os.status = 'DELIVERED' " +
           "AND os.deliveredAt >= :start AND os.deliveredAt < :end")
    List<TechnicalOs> findDeliveredByGroomerAndDateRange(
            @Param("groomerId") Long groomerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
