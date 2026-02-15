package com.cisnebranco.controller;

import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.HealthChecklistRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class TechnicalOsController {

    private final TechnicalOsService osService;
    private final InspectionPhotoService photoService;
    private final HealthChecklistService checklistService;

    @PostMapping("/check-in")
    public ResponseEntity<TechnicalOsResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(osService.checkIn(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TechnicalOsResponse> updateStatus(@PathVariable Long id,
                                                             @Valid @RequestBody OsStatusUpdateRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.ok(osService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/groomer/{groomerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicalOsResponse> assignGroomer(@PathVariable Long id,
                                                              @PathVariable Long groomerId) {
        return ResponseEntity.ok(osService.assignGroomer(id, groomerId));
    }

    @GetMapping
    public ResponseEntity<?> findAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == UserRole.GROOMER) {
            requireGroomerProfile(principal);
            List<TechnicalOsGroomerViewResponse> result = osService.findByGroomer(principal.getGroomerId());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(osService.findAll());
    }

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

    @PostMapping("/{id}/photos")
    public ResponseEntity<InspectionPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.upload(id, file, caption));
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<InspectionPhotoResponse>> getPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(photoService.findByOs(id));
    }

    // --- Health checklist ---

    @PostMapping("/{id}/checklist")
    public ResponseEntity<HealthChecklistResponse> saveChecklist(
            @PathVariable Long id,
            @Valid @RequestBody HealthChecklistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        osService.enforceAccess(id, principal);
        return ResponseEntity.ok(checklistService.createOrUpdate(id, request));
    }

    @GetMapping("/{id}/checklist")
    public ResponseEntity<HealthChecklistResponse> getChecklist(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.findByOs(id));
    }

    private void requireGroomerProfile(UserPrincipal principal) {
        if (principal.getGroomerId() == null) {
            throw new BusinessException("User account is not linked to a groomer profile. Contact an administrator.");
        }
    }
}
