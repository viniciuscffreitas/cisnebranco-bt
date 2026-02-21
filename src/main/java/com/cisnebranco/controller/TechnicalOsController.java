package com.cisnebranco.controller;

import com.cisnebranco.dto.request.AdjustServiceItemPriceRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.HealthChecklistRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
import com.cisnebranco.dto.request.TechnicalOsFilterRequest;
import com.cisnebranco.dto.response.AuditLogResponse;
import com.cisnebranco.dto.response.HealthChecklistResponse;
import com.cisnebranco.dto.response.InspectionPhotoResponse;
import com.cisnebranco.dto.response.TechnicalOsGroomerViewResponse;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.HealthChecklistService;
import com.cisnebranco.service.InspectionPhotoService;
import com.cisnebranco.service.TechnicalOsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/os")
@RequiredArgsConstructor
@Tag(name = "Service Orders", description = "Technical OS (service order) management")
public class TechnicalOsController {

    private final TechnicalOsService osService;
    private final InspectionPhotoService photoService;
    private final HealthChecklistService checklistService;

    @Operation(summary = "Check in a pet and create a new service order")
    @PostMapping("/check-in")
    public ResponseEntity<TechnicalOsResponse> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(osService.checkIn(request, principal.getId()));
    }

    @Operation(summary = "Update the status of a service order")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TechnicalOsResponse> updateStatus(@PathVariable Long id,
                                                             @Valid @RequestBody OsStatusUpdateRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.ok(osService.updateStatus(id, request));
    }

    @Operation(summary = "Assign a groomer to a service order")
    @PatchMapping("/{id}/groomer/{groomerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicalOsResponse> assignGroomer(@PathVariable Long id,
                                                              @PathVariable Long groomerId) {
        return ResponseEntity.ok(osService.assignGroomer(id, groomerId));
    }

    @Operation(summary = "List service orders with optional filters")
    @GetMapping
    public ResponseEntity<?> findAll(@AuthenticationPrincipal UserPrincipal principal,
                                      @ModelAttribute TechnicalOsFilterRequest filter,
                                      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (principal.getRole() == UserRole.GROOMER) {
            requireGroomerProfile(principal);
            Page<TechnicalOsGroomerViewResponse> result = osService.findByGroomer(principal.getGroomerId(), pageable);
            return ResponseEntity.ok(result);
        }
        boolean hasFilters = filter.status() != null || filter.groomerId() != null
                || filter.clientId() != null || filter.petId() != null
                || filter.paymentStatus() != null
                || filter.startDate() != null || filter.endDate() != null;
        if (hasFilters) {
            return ResponseEntity.ok(osService.findByFilters(filter, pageable));
        }
        return ResponseEntity.ok(osService.findAll(pageable));
    }

    @Operation(summary = "Find a service order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == UserRole.GROOMER) {
            requireGroomerProfile(principal);
            return ResponseEntity.ok(osService.findByIdForGroomer(id, principal.getGroomerId()));
        }
        return ResponseEntity.ok(osService.findById(id));
    }

    // --- Photos ---

    @Operation(summary = "Upload an inspection photo for a service order")
    @PostMapping("/{id}/photos")
    public ResponseEntity<InspectionPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.upload(id, file, caption));
    }

    @Operation(summary = "List inspection photos for a service order")
    @GetMapping("/{id}/photos")
    public ResponseEntity<List<InspectionPhotoResponse>> getPhotos(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.ok(photoService.findByOs(id));
    }

    // --- Service item price adjustment ---

    @Operation(summary = "Adjust the price of a service item (increase only)")
    @PatchMapping("/{osId}/services/{itemId}/price")
    public ResponseEntity<TechnicalOsResponse> adjustServiceItemPrice(
            @PathVariable Long osId,
            @PathVariable Long itemId,
            @Valid @RequestBody AdjustServiceItemPriceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(osId, principal);
        return ResponseEntity.ok(osService.adjustServiceItemPrice(osId, itemId, request));
    }

    // --- Health checklist ---

    @Operation(summary = "Create or update the health checklist for a service order")
    @PostMapping("/{id}/checklist")
    public ResponseEntity<HealthChecklistResponse> saveChecklist(
            @PathVariable Long id,
            @Valid @RequestBody HealthChecklistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.ok(checklistService.createOrUpdate(id, request));
    }

    @Operation(summary = "Get the health checklist for a service order")
    @GetMapping("/{id}/checklist")
    public ResponseEntity<HealthChecklistResponse> getChecklist(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.findByOs(id));
    }

    // --- Audit log ---

    @Operation(summary = "Get the immutable audit log for a service order (admin only)")
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(osService.getOsAuditLog(id));
    }

    private void requireGroomerProfile(UserPrincipal principal) {
        if (principal.getGroomerId() == null) {
            throw new BusinessException("User account is not linked to a groomer profile. Contact an administrator.");
        }
    }
}
