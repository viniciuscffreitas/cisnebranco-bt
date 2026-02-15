package com.cisnebranco.controller;

import com.cisnebranco.dto.request.CommissionGenerateRequest;
import com.cisnebranco.dto.response.WeeklyCommissionResponse;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.WeeklyCommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commissions")
@RequiredArgsConstructor
public class WeeklyCommissionController {

    private final WeeklyCommissionService commissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklyCommissionResponse> generate(@Valid @RequestBody CommissionGenerateRequest request) {
        return ResponseEntity.ok(commissionService.generateForWeek(request.groomerId(), request.weekStart()));
    }

    @GetMapping
    public ResponseEntity<List<WeeklyCommissionResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == UserRole.GROOMER) {
            if (principal.getGroomerId() == null) {
                throw new BusinessException("User account is not linked to a groomer profile. Contact an administrator.");
            }
            return ResponseEntity.ok(commissionService.findByGroomer(principal.getGroomerId()));
        }
        return ResponseEntity.ok(commissionService.findAll());
    }

    @GetMapping("/groomer/{groomerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WeeklyCommissionResponse>> findByGroomer(@PathVariable Long groomerId) {
        return ResponseEntity.ok(commissionService.findByGroomer(groomerId));
    }
}
