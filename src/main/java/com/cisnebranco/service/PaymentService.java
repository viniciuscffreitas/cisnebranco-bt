package com.cisnebranco.service;

import com.cisnebranco.dto.request.PaymentRequest;
import com.cisnebranco.dto.response.PaymentEventResponse;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.PaymentEvent;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.PaymentStatus;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.PaymentEventMapper;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.PaymentEventRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentEventRepository paymentEventRepository;
    private final TechnicalOsRepository osRepository;
    private final AppUserRepository userRepository;
    private final PaymentEventMapper paymentEventMapper;
    private final AuditService auditService;

    @Transactional
    public PaymentEventResponse recordPayment(Long osId, PaymentRequest request, Long userId) {
        // Pessimistic lock prevents concurrent payments from exceeding total
        TechnicalOs os = osRepository.findByIdForUpdate(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        if (os.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Cannot record payment for a cancelled OS");
        }
        if (os.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("Cannot record payment for a refunded OS");
        }

        BigDecimal newTotal = os.getTotalPaid().add(request.amount());
        if (newTotal.compareTo(os.getTotalPrice()) > 0) {
            throw new BusinessException("Payment amount exceeds remaining balance. Remaining: "
                    + os.getTotalPrice().subtract(os.getTotalPaid()));
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        PaymentEvent event = new PaymentEvent();
        event.setTechnicalOs(os);
        event.setAmount(request.amount());
        event.setMethod(request.method());
        event.setTransactionRef(request.transactionRef());
        event.setNotes(request.notes());
        event.setCreatedBy(user);

        paymentEventRepository.save(event);

        // Update total_paid — the DB trigger will auto-update payment_status
        os.setTotalPaid(newTotal);
        osRepository.save(os);

        auditService.log("PAYMENT_RECORDED", "TechnicalOs", osId,
                "Amount: " + request.amount() + " via " + request.method());

        return paymentEventMapper.toResponse(event);
    }

    @Transactional
    public PaymentEventResponse refundPayment(Long osId, Long eventId, Long userId) {
        PaymentEvent original = paymentEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentEvent", eventId));

        // Validate the event belongs to the specified OS
        if (!original.getTechnicalOs().getId().equals(osId)) {
            throw new BusinessException("Payment event does not belong to the specified OS");
        }

        if (original.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Cannot refund a refund event");
        }

        // Prevent double refund — unique index also enforces this at DB level
        if (paymentEventRepository.existsByRefundOfId(eventId)) {
            throw new BusinessException("This payment has already been refunded");
        }

        // Pessimistic lock on the OS to prevent concurrent refund/payment race
        TechnicalOs os = osRepository.findByIdForUpdate(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        BigDecimal newTotal = os.getTotalPaid().subtract(original.getAmount());
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Refund would result in negative total paid");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        PaymentEvent refund = new PaymentEvent();
        refund.setTechnicalOs(os);
        refund.setAmount(original.getAmount().negate());
        refund.setMethod(original.getMethod());
        refund.setTransactionRef(original.getTransactionRef());
        refund.setNotes("Refund of payment #" + original.getId());
        refund.setRefundOf(original);
        refund.setCreatedBy(user);

        paymentEventRepository.save(refund);

        os.setTotalPaid(newTotal);
        osRepository.save(os);

        auditService.log("PAYMENT_REFUNDED", "TechnicalOs", osId,
                "Refund of payment #" + original.getId() + " amount: " + original.getAmount());

        return paymentEventMapper.toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getPaymentHistory(Long osId) {
        if (!osRepository.existsById(osId)) {
            throw new ResourceNotFoundException("TechnicalOs", osId);
        }
        return paymentEventRepository.findByTechnicalOsIdOrderByCreatedAtDesc(osId)
                .stream()
                .map(paymentEventMapper::toResponse)
                .toList();
    }
}
