package com.cisnebranco.service;

import com.cisnebranco.dto.request.ServiceTypeRequest;
import com.cisnebranco.dto.response.ServiceTypeResponse;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.ServiceTypeMapper;
import com.cisnebranco.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
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
        sseEmitterService.broadcastAfterCommit("service-type-changed", "created", saved.getId());
        return serviceTypeMapper.toResponse(saved);
    }

    @Transactional
    public ServiceTypeResponse update(Long id, ServiceTypeRequest request) {
        ServiceType serviceType = findEntityById(id);
        serviceTypeMapper.updateEntity(request, serviceType);
        sseEmitterService.broadcastAfterCommit("service-type-changed", "updated", id);
        return serviceTypeMapper.toResponse(serviceTypeRepository.save(serviceType));
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceType serviceType = findEntityById(id);
        serviceType.setActive(false);
        serviceTypeRepository.save(serviceType);
        sseEmitterService.broadcastAfterCommit("service-type-changed", "deactivated", id);
    }

    private ServiceType findActiveEntityById(Long id) {
        return serviceTypeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", id));
    }

    private ServiceType findEntityById(Long id) {
        return serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", id));
    }
}
