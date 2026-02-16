package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PaymentRequest;
import com.cisnebranco.dto.response.PaymentEventResponse;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment recording and refund management for service orders")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Record a payment for a service order")
    @PostMapping
    public ResponseEntity<PaymentEventResponse> recordPayment(
            @PathVariable Long osId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordPayment(osId, request, principal.getId()));
    }

    @Operation(summary = "Get payment history for a service order")
    @GetMapping
    public ResponseEntity<List<PaymentEventResponse>> getPaymentHistory(@PathVariable Long osId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(osId));
    }

    @Operation(summary = "Refund a specific payment event")
    @PostMapping("/{eventId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentEventResponse> refundPayment(
            @PathVariable Long osId,
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.refundPayment(osId, eventId, principal.getId()));
    }
}
