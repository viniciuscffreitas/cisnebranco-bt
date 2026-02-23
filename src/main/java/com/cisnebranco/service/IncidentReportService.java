package com.cisnebranco.service;

import com.cisnebranco.dto.request.CreateIncidentRequest;
import com.cisnebranco.dto.response.IncidentReportResponse;
import com.cisnebranco.entity.IncidentMedia;
import com.cisnebranco.entity.IncidentReport;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.IncidentCategory;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.IncidentReportMapper;
import com.cisnebranco.repository.IncidentReportRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentReportService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "video/mp4", "video/quicktime"
    );

    private static final EnumSet<OsStatus> ALLOWED_STATUSES = EnumSet.of(
            OsStatus.IN_PROGRESS, OsStatus.READY, OsStatus.DELIVERED
    );

    private final IncidentReportRepository incidentRepository;
    private final TechnicalOsRepository osRepository;
    private final IncidentReportMapper incidentMapper;
    private final AuditService auditService;
    private final SseEmitterService sseEmitterService;

    @Value("${app.upload.photo-dir}")
    private String uploadDir;

    @Transactional
    public IncidentReportResponse create(Long osId, CreateIncidentRequest request,
                                         List<MultipartFile> files, String username) {
        TechnicalOs os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        if (!ALLOWED_STATUSES.contains(os.getStatus())) {
            throw new BusinessException(
                    "Incidents can only be reported when the OS is IN_PROGRESS, READY, or DELIVERED");
        }

        IncidentCategory category;
        try {
            category = IncidentCategory.valueOf(request.category());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid incident category: " + request.category());
        }

        IncidentReport incident = new IncidentReport();
        incident.setTechnicalOs(os);
        incident.setCategory(category);
        incident.setDescription(request.description());
        incident.setReportedBy(username);

        // Handle file uploads
        List<Path> savedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            Path dir = Paths.get(uploadDir, "incidents", String.valueOf(osId))
                    .toAbsolutePath().normalize();

            for (MultipartFile file : files) {
                if (file.getContentType() == null
                        || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                    throw new BusinessException(
                            "Invalid file type. Allowed: JPEG, PNG, WebP, MP4, QuickTime");
                }

                String originalFilename = file.getOriginalFilename();
                String safeName = (originalFilename != null)
                        ? Paths.get(originalFilename).getFileName().toString()
                        : "upload";
                String filename = UUID.randomUUID() + "_" + safeName;

                Path targetFile = dir.resolve(filename).normalize();
                if (!targetFile.startsWith(dir)) {
                    throw new BusinessException("Invalid file path");
                }

                try {
                    Files.createDirectories(dir);
                    Files.copy(file.getInputStream(), targetFile);
                    savedFiles.add(targetFile);
                } catch (IOException e) {
                    // Clean up all files saved so far
                    cleanupFiles(savedFiles);
                    throw new BusinessException("Failed to save file: " + e.getMessage(), e);
                }

                IncidentMedia media = new IncidentMedia();
                media.setIncidentReport(incident);
                media.setFilePath(targetFile.toString());
                media.setContentType(file.getContentType());
                incident.getMedia().add(media);
            }
        }

        try {
            IncidentReport saved = incidentRepository.save(incident);

            auditService.log("INCIDENT_REPORTED", "TechnicalOs", osId,
                    "Incident: " + category + " — " + request.description());

            sseEmitterService.broadcastAfterCommit("os-status-changed", "INCIDENT_REPORTED", osId);

            return incidentMapper.toResponse(saved);
        } catch (Exception e) {
            cleanupFiles(savedFiles);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<IncidentReportResponse> findByOs(Long osId) {
        return incidentRepository.findByTechnicalOsIdOrderByCreatedAtDesc(osId).stream()
                .map(incidentMapper::toResponse)
                .toList();
    }

    private void cleanupFiles(List<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException cleanupEx) {
                log.warn("Failed to clean up file after failure: {}", file, cleanupEx);
            }
        }
    }
}
