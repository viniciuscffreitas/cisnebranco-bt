package com.cisnebranco.controller;

import com.cisnebranco.dto.request.AppointmentRequest;
import com.cisnebranco.dto.request.AppointmentUpdateRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.response.AppointmentResponse;
import com.cisnebranco.dto.response.TimeSlot;
import com.cisnebranco.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<TimeSlot>> getAvailableSlots(
            @RequestParam Long groomerId,
            @RequestParam Long serviceTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(groomerId, serviceTypeId, date));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(appointmentService.findByDateRange(startDate, endDate));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AppointmentResponse>> findByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(appointmentService.findByClient(clientId));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<AppointmentResponse> convertToOs(
            @PathVariable Long id,
            @Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(appointmentService.convertToOs(id, request));
    }
}
