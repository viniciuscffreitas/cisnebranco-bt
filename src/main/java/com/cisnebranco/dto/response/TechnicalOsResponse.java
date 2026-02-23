package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TechnicalOsResponse(
        Long id,
        PetResponse pet,
        GroomerResponse groomer,
        OsStatus status,
        BigDecimal totalPrice,
        BigDecimal totalCommission,
        BigDecimal balance,
        PaymentStatus paymentStatus,
        BigDecimal totalPaid,
        BigDecimal paymentBalance,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime deliveredAt,
        String notes,
        List<OsServiceItemResponse> serviceItems,
        List<InspectionPhotoResponse> photos,
        List<PaymentEventResponse> paymentEvents,
        HealthChecklistResponse healthChecklist,
        LocalDateTime createdAt,
        int incidentCount,
        LocalDateTime scheduledStart
) {}
