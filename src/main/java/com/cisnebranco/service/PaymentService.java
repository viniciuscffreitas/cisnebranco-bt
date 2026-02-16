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

    @Transactional
    public PaymentEventResponse recordPayment(Long osId, PaymentRequest request, Long userId) {
        TechnicalOs os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        if (os.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Cannot record payment for a cancelled OS");
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

        return paymentEventMapper.toResponse(event);
    }

    @Transactional
    public PaymentEventResponse refundPayment(Long eventId, Long userId) {
        PaymentEvent original = paymentEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentEvent", eventId));

        TechnicalOs os = original.getTechnicalOs();

        if (original.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Cannot refund a refund event");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        PaymentEvent refund = new PaymentEvent();
        refund.setTechnicalOs(os);
        refund.setAmount(original.getAmount().negate());
        refund.setMethod(original.getMethod());
        refund.setTransactionRef(original.getTransactionRef());
        refund.setNotes("Refund of payment #" + original.getId());
        refund.setCreatedBy(user);

        paymentEventRepository.save(refund);

        os.setTotalPaid(os.getTotalPaid().subtract(original.getAmount()));
        osRepository.save(os);

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
