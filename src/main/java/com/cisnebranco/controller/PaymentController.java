package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PaymentRequest;
import com.cisnebranco.dto.response.PaymentEventResponse;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/os/{osId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentEventResponse> recordPayment(
            @PathVariable Long osId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordPayment(osId, request, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<List<PaymentEventResponse>> getPaymentHistory(@PathVariable Long osId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(osId));
    }

    @PostMapping("/{eventId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentEventResponse> refundPayment(
            @PathVariable Long osId,
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.refundPayment(eventId, principal.getId()));
    }
}
