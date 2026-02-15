package com.cisnebranco.service;

import com.cisnebranco.dto.request.BreedRequest;
import com.cisnebranco.dto.response.BreedResponse;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.BreedMapper;
import com.cisnebranco.repository.BreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BreedService {

    private final BreedRepository breedRepository;
    private final BreedMapper breedMapper;

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
        return breedMapper.toResponse(breedRepository.save(breed));
    }

    @Transactional
    public BreedResponse update(Long id, BreedRequest request) {
        Breed breed = findEntityById(id);
        breedMapper.updateEntity(request, breed);
        return breedMapper.toResponse(breedRepository.save(breed));
    }

    @Transactional
    public void delete(Long id) {
        Breed breed = findEntityById(id);
        breedRepository.delete(breed);
    }

    private Breed findEntityById(Long id) {
        return breedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Breed", id));
    }
}
