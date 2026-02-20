package com.cisnebranco.service;

import com.cisnebranco.dto.request.HealthChecklistRequest;
import com.cisnebranco.dto.response.HealthChecklistResponse;
import com.cisnebranco.entity.HealthChecklist;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.HealthChecklistMapper;
import com.cisnebranco.repository.HealthChecklistRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthChecklistService {

    private final HealthChecklistRepository checklistRepository;
    private final TechnicalOsRepository osRepository;
    private final HealthChecklistMapper checklistMapper;
    private final AuditService auditService;

    @Transactional
    public HealthChecklistResponse createOrUpdate(Long osId, HealthChecklistRequest request) {
        TechnicalOs os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        HealthChecklist checklist = checklistRepository.findByTechnicalOsId(osId)
                .orElseGet(() -> {
                    HealthChecklist c = new HealthChecklist();
                    c.setTechnicalOs(os);
                    return c;
                });

        checklistMapper.updateEntity(request, checklist);
        HealthChecklistResponse response = checklistMapper.toResponse(checklistRepository.save(checklist));
        auditService.log("ACEITE_VISTORIA", "TechnicalOs", osId,
                "Groomer registrou/atualizou o checklist de saúde da OS #" + osId);
        return response;
    }

    @Transactional(readOnly = true)
    public HealthChecklistResponse findByOs(Long osId) {
        HealthChecklist checklist = checklistRepository.findByTechnicalOsId(osId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthChecklist for OS " + osId));
        return checklistMapper.toResponse(checklist);
    }
}
