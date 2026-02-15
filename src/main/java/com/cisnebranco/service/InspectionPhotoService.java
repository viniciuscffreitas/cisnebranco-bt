package com.cisnebranco.service;

import com.cisnebranco.dto.response.InspectionPhotoResponse;
import com.cisnebranco.entity.InspectionPhoto;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.InspectionPhotoRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionPhotoService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final InspectionPhotoRepository photoRepository;
    private final TechnicalOsRepository osRepository;

    @Value("${app.upload.photo-dir}")
    private String photoDir;

    @Transactional
    public InspectionPhotoResponse upload(Long osId, MultipartFile file, String caption) {
        TechnicalOs os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException("Invalid file type. Allowed: JPEG, PNG, WebP");
        }

        String originalFilename = file.getOriginalFilename();
        String safeName = (originalFilename != null)
                ? Paths.get(originalFilename).getFileName().toString()
                : "upload";
        String filename = UUID.randomUUID() + "_" + safeName;

        Path dir = Paths.get(photoDir, String.valueOf(osId)).toAbsolutePath().normalize();
        Path targetFile = dir.resolve(filename).normalize();

        if (!targetFile.startsWith(dir)) {
            throw new BusinessException("Invalid file path");
        }

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), targetFile);
        } catch (IOException e) {
            throw new BusinessException("Failed to save photo", e);
        }

        try {
            InspectionPhoto photo = new InspectionPhoto();
            photo.setTechnicalOs(os);
            photo.setFilePath(targetFile.toString());
            photo.setCaption(caption);

            InspectionPhoto saved = photoRepository.save(photo);
            return toResponse(saved);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetFile);
            } catch (IOException cleanupEx) {
                log.warn("Failed to clean up orphaned file: {}", targetFile, cleanupEx);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<InspectionPhotoResponse> findByOs(Long osId) {
        return photoRepository.findByTechnicalOsId(osId).stream()
                .map(this::toResponse)
                .toList();
    }

    private InspectionPhotoResponse toResponse(InspectionPhoto photo) {
        return new InspectionPhotoResponse(
                photo.getId(),
                photo.getTechnicalOs().getId(),
                photo.getFilePath(),
                photo.getCaption()
        );
    }
}
