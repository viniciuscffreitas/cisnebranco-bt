package com.cisnebranco.controller;

import com.cisnebranco.dto.request.CommissionGenerateRequest;
import com.cisnebranco.dto.response.WeeklyCommissionResponse;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.WeeklyCommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/commissions")
@RequiredArgsConstructor
@Tag(name = "Commissions", description = "Weekly groomer commission management")
public class WeeklyCommissionController {

    private final WeeklyCommissionService commissionService;

    @Operation(summary = "Generate a weekly commission for a groomer")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklyCommissionResponse> generate(@Valid @RequestBody CommissionGenerateRequest request) {
        return ResponseEntity.ok(commissionService.generateForWeek(request.groomerId(), request.weekStart()));
    }

    @Operation(summary = "List commissions with pagination (filtered by role)")
    @GetMapping
    public ResponseEntity<Page<WeeklyCommissionResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "weekStart") Pageable pageable) {
        if (principal.getRole() == UserRole.GROOMER) {
            if (principal.getGroomerId() == null) {
                throw new BusinessException("User account is not linked to a groomer profile. Contact an administrator.");
            }
            return ResponseEntity.ok(commissionService.findByGroomer(principal.getGroomerId(), pageable));
        }
        return ResponseEntity.ok(commissionService.findAll(pageable));
    }

    @Operation(summary = "List commissions for a specific groomer")
    @GetMapping("/groomer/{groomerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WeeklyCommissionResponse>> findByGroomer(@PathVariable Long groomerId,
                                                                         @PageableDefault(size = 20, sort = "weekStart") Pageable pageable) {
        return ResponseEntity.ok(commissionService.findByGroomer(groomerId, pageable));
    }
}
