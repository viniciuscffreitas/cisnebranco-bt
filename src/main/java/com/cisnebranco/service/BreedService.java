package com.cisnebranco.service;

import com.cisnebranco.dto.request.BreedRequest;
import com.cisnebranco.dto.response.BreedResponse;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.BreedMapper;
import com.cisnebranco.repository.BreedRepository;
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
public class BreedService {

    private final BreedRepository breedRepository;
    private final BreedMapper breedMapper;
    private final SseEmitterService sseEmitterService;

    @Transactional(readOnly = true)
    public List<BreedResponse> findAll() {
        return breedRepository.findAll().stream()
                .map(breedMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BreedResponse findById(Long id) {
        return breedMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public BreedResponse create(BreedRequest request) {
        Breed breed = breedMapper.toEntity(request);
        Breed saved = breedRepository.save(breed);
        broadcastEvent("breed-changed", "created", saved.getId());
        return breedMapper.toResponse(saved);
    }

    @Transactional
    public BreedResponse update(Long id, BreedRequest request) {
        Breed breed = findEntityById(id);
        breedMapper.updateEntity(request, breed);
        broadcastEvent("breed-changed", "updated", id);
        return breedMapper.toResponse(breedRepository.save(breed));
    }

    @Transactional
    public void delete(Long id) {
        Breed breed = findEntityById(id);
        Long breedId = breed.getId();
        breedRepository.delete(breed);
        broadcastEvent("breed-changed", "deleted", breedId);
    }

    private Breed findEntityById(Long id) {
        return breedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Breed", id));
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
