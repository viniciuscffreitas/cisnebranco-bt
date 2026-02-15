package com.cisnebranco.repository;

import com.cisnebranco.entity.InspectionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionPhotoRepository extends JpaRepository<InspectionPhoto, Long> {
    List<InspectionPhoto> findByTechnicalOsId(Long technicalOsId);
    long countByTechnicalOsId(Long technicalOsId);
}
