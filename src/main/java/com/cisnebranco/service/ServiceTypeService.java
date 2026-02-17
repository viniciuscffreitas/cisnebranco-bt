package com.cisnebranco.service;

import com.cisnebranco.dto.request.ServiceTypeRequest;
import com.cisnebranco.dto.response.ServiceTypeResponse;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.ServiceTypeMapper;
import com.cisnebranco.repository.ServiceTypeRepository;
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
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;
    private final SseEmitterService sseEmitterService;

    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> findAll() {
        return serviceTypeRepository.findByActiveTrue().stream()
                .map(serviceTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceTypeResponse findById(Long id) {
        return serviceTypeMapper.toResponse(findActiveEntityById(id));
    }

    @Transactional
    public ServiceTypeResponse create(ServiceTypeRequest request) {
        ServiceType serviceType = serviceTypeMapper.toEntity(request);
        ServiceType saved = serviceTypeRepository.save(serviceType);
        broadcastEvent("service-type-changed", "created", saved.getId());
        return serviceTypeMapper.toResponse(saved);
    }

    @Transactional
    public ServiceTypeResponse update(Long id, ServiceTypeRequest request) {
        ServiceType serviceType = findEntityById(id);
        serviceTypeMapper.updateEntity(request, serviceType);
        broadcastEvent("service-type-changed", "updated", id);
        return serviceTypeMapper.toResponse(serviceTypeRepository.save(serviceType));
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceType serviceType = findEntityById(id);
        serviceType.setActive(false);
        serviceTypeRepository.save(serviceType);
        broadcastEvent("service-type-changed", "deactivated", id);
    }

    private ServiceType findActiveEntityById(Long id) {
        return serviceTypeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", id));
    }

    private ServiceType findEntityById(Long id) {
        return serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", id));
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
