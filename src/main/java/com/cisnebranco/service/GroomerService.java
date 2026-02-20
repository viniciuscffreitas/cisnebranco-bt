package com.cisnebranco.service;

import com.cisnebranco.dto.request.GroomerRequest;
import com.cisnebranco.dto.response.GroomerResponse;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.GroomerMapper;
import com.cisnebranco.repository.GroomerRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroomerService {

    private static final List<OsStatus> ACTIVE_STATUSES =
            List.of(OsStatus.SCHEDULED, OsStatus.WAITING, OsStatus.IN_PROGRESS, OsStatus.READY);

    private final GroomerRepository groomerRepository;
    private final TechnicalOsRepository technicalOsRepository;
    private final GroomerMapper groomerMapper;
    private final SseEmitterService sseEmitterService;

    @Transactional(readOnly = true)
    public List<GroomerResponse> findAll() {
        return groomerRepository.findAll().stream()
                .map(groomerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroomerResponse> findActive() {
        return groomerRepository.findByActiveTrue().stream()
                .map(groomerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroomerResponse findById(Long id) {
        return groomerMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public GroomerResponse create(GroomerRequest request) {
        Groomer groomer = groomerMapper.toEntity(request);
        Groomer saved = groomerRepository.save(groomer);
        sseEmitterService.broadcastAfterCommit("groomer-changed", "created", saved.getId());
        return groomerMapper.toResponse(saved);
    }

    @Transactional
    public GroomerResponse update(Long id, GroomerRequest request) {
        Groomer groomer = findEntityById(id);
        groomerMapper.updateEntity(request, groomer);
        sseEmitterService.broadcastAfterCommit("groomer-changed", "updated", id);
        return groomerMapper.toResponse(groomerRepository.save(groomer));
    }

    @Transactional
    public void deactivate(Long id) {
        Groomer groomer = findEntityById(id);
        int activeCount = technicalOsRepository.countByGroomerIdAndStatusIn(id, ACTIVE_STATUSES);
        if (activeCount > 0) {
            throw new BusinessException(
                    String.format("Groomer possui %d OS(s) ativas. Reatribua antes de desativar.", activeCount));
        }
        groomer.setActive(false);
        groomerRepository.save(groomer);
        sseEmitterService.broadcastAfterCommit("groomer-changed", "deactivated", id);
    }

    private Groomer findEntityById(Long id) {
        return groomerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", id));
    }
}
