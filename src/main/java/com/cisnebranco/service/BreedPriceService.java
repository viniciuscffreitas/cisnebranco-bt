package com.cisnebranco.service;

import com.cisnebranco.dto.request.BreedServicePriceRequest;
import com.cisnebranco.dto.response.BreedServicePriceResponse;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.entity.ServiceTypeBreedPrice;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.BreedRepository;
import com.cisnebranco.repository.ServiceTypeBreedPriceRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BreedPriceService {

    private final ServiceTypeBreedPriceRepository priceRepository;
    private final BreedRepository breedRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    @Transactional(readOnly = true)
    public List<BreedServicePriceResponse> getServicePricesForBreed(Long breedId) {
        if (!breedRepository.existsById(breedId)) {
            throw new ResourceNotFoundException("Breed", breedId);
        }
        return priceRepository.findByBreedId(breedId).stream()
                .map(p -> toResponse(p.getBreed().getId(), p.getServiceType(), p.getPrice()))
                .toList();
    }

    @Transactional
    public BreedServicePriceResponse upsertServicePrice(Long breedId, Long serviceTypeId,
                                                        BreedServicePriceRequest request) {
        Breed breed = breedRepository.findById(breedId)
                .orElseThrow(() -> new ResourceNotFoundException("Breed", breedId));
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));
        if (!serviceType.isActive()) {
            throw new BusinessException("Cannot set price for inactive service type: " + serviceType.getName());
        }

        ServiceTypeBreedPrice price = priceRepository
                .findByServiceTypeIdAndBreedId(serviceTypeId, breedId)
                .orElseGet(() -> {
                    ServiceTypeBreedPrice p = new ServiceTypeBreedPrice();
                    p.setBreed(breed);
                    p.setServiceType(serviceType);
                    return p;
                });

        price.setPrice(request.price());
        ServiceTypeBreedPrice saved = priceRepository.save(price);
        return toResponse(breedId, serviceType, saved.getPrice());
    }

    private BreedServicePriceResponse toResponse(Long breedId, ServiceType st,
                                                  java.math.BigDecimal breedPrice) {
        return new BreedServicePriceResponse(
                breedId,
                st.getId(),
                st.getCode(),
                st.getName(),
                st.getDefaultDurationMinutes(),
                st.getBasePrice(),
                breedPrice
        );
    }
}
