package com.cisnebranco.service;

import com.cisnebranco.dto.response.InspectionPhotoResponse;
import com.cisnebranco.entity.InspectionPhoto;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.InspectionPhotoRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspectionPhotoService {

    private final InspectionPhotoRepository photoRepository;
    private final TechnicalOsRepository osRepository;

    @Value("${app.upload.photo-dir}")
    private String photoDir;

    @Transactional
    public InspectionPhotoResponse upload(Long osId, MultipartFile file, String caption) {
        TechnicalOs os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dir = Paths.get(photoDir, String.valueOf(osId));

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
        } catch (IOException e) {
            throw new BusinessException("Failed to save photo: " + e.getMessage());
        }

        InspectionPhoto photo = new InspectionPhoto();
        photo.setTechnicalOs(os);
        photo.setFilePath(dir.resolve(filename).toString());
        photo.setCaption(caption);

        InspectionPhoto saved = photoRepository.save(photo);
        return toResponse(saved);
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
