package com.cisnebranco.service;

import com.cisnebranco.dto.response.BreedServicePriceResponse;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.BreedRepository;
import com.cisnebranco.repository.ServiceTypeBreedPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BreedPriceService {

    private final ServiceTypeBreedPriceRepository priceRepository;
    private final BreedRepository breedRepository;

    @Transactional(readOnly = true)
    public List<BreedServicePriceResponse> getServicePricesForBreed(Long breedId) {
        if (!breedRepository.existsById(breedId)) {
            throw new ResourceNotFoundException("Breed", breedId);
        }
        return priceRepository.findByBreedId(breedId).stream()
                .map(p -> new BreedServicePriceResponse(
                        p.getBreed().getId(),
                        p.getServiceType().getId(),
                        p.getServiceType().getCode(),
                        p.getServiceType().getName(),
                        p.getServiceType().getDefaultDurationMinutes(),
                        p.getPrice()
                ))
                .toList();
    }
}
