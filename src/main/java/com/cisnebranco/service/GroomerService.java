package com.cisnebranco.service;

import com.cisnebranco.dto.request.GroomerRequest;
import com.cisnebranco.dto.response.GroomerResponse;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.GroomerMapper;
import com.cisnebranco.repository.GroomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroomerService {

    private final GroomerRepository groomerRepository;
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
        broadcastEvent("groomer-changed", "created", saved.getId());
        return groomerMapper.toResponse(saved);
    }

    @Transactional
    public GroomerResponse update(Long id, GroomerRequest request) {
        Groomer groomer = findEntityById(id);
        groomerMapper.updateEntity(request, groomer);
        broadcastEvent("groomer-changed", "updated", id);
        return groomerMapper.toResponse(groomerRepository.save(groomer));
    }

    @Transactional
    public void deactivate(Long id) {
        Groomer groomer = findEntityById(id);
        groomer.setActive(false);
        groomerRepository.save(groomer);
        broadcastEvent("groomer-changed", "deactivated", id);
    }

    private Groomer findEntityById(Long id) {
        return groomerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", id));
    }

    private void broadcastEvent(String eventName, String action, Long id) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sseEmitterService.sendToAll(eventName, Map.of("action", action, "id", id));
                } catch (Exception e) {
                    log.warn("Failed to broadcast SSE event '{}' for id {}", eventName, id, e);
                }
            }
        });
    }
}
