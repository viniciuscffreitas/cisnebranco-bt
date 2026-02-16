package com.cisnebranco.repository;

import com.cisnebranco.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByTechnicalOsIdOrderByCreatedAtDesc(Long technicalOsId);

    @Query("SELECT COALESCE(SUM(pe.amount), 0) FROM PaymentEvent pe WHERE pe.technicalOs.id = :osId")
    BigDecimal sumAmountByTechnicalOsId(@Param("osId") Long osId);
}
