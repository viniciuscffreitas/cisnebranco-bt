package com.cisnebranco.service;

import com.cisnebranco.dto.request.PricingMatrixRequest;
import com.cisnebranco.dto.response.PricingMatrixResponse;
import com.cisnebranco.entity.PricingMatrix;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.PricingMatrixMapper;
import com.cisnebranco.repository.PricingMatrixRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingMatrixService {

    private final PricingMatrixRepository pricingMatrixRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PricingMatrixMapper pricingMatrixMapper;

    @Transactional(readOnly = true)
    public List<PricingMatrixResponse> findAll() {
        return pricingMatrixRepository.findAll().stream()
                .map(pricingMatrixMapper::toResponse)
                .toList();
    }

    @Transactional
    public PricingMatrixResponse createOrUpdate(PricingMatrixRequest request) {
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", request.serviceTypeId()));

        PricingMatrix pm = pricingMatrixRepository
                .findByServiceTypeIdAndSpeciesAndPetSize(request.serviceTypeId(), request.species(), request.petSize())
                .orElseGet(PricingMatrix::new);

        pm.setServiceType(serviceType);
        pm.setSpecies(request.species());
        pm.setPetSize(request.petSize());
        pm.setPrice(request.price());

        return pricingMatrixMapper.toResponse(pricingMatrixRepository.save(pm));
    }

    @Transactional
    public void delete(Long id) {
        PricingMatrix pm = pricingMatrixRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PricingMatrix", id));
        pricingMatrixRepository.delete(pm);
    }
}
