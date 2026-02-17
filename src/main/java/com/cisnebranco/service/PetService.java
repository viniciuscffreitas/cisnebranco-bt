package com.cisnebranco.service;

import com.cisnebranco.dto.request.PetRequest;
import com.cisnebranco.dto.response.PetGroomerViewResponse;
import com.cisnebranco.dto.response.PetResponse;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.entity.Client;
import com.cisnebranco.entity.Pet;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.PetMapper;
import com.cisnebranco.repository.BreedRepository;
import com.cisnebranco.repository.ClientRepository;
import com.cisnebranco.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final ClientRepository clientRepository;
    private final BreedRepository breedRepository;
    private final PetMapper petMapper;
    private final SseEmitterService sseEmitterService;

    @Transactional(readOnly = true)
    public Page<PetResponse> findAll(Pageable pageable) {
        return petRepository.findByActiveTrue(pageable)
                .map(petMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PetResponse> findByClient(Long clientId) {
        return petRepository.findByClientIdAndActiveTrue(clientId).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetResponse findById(Long id) {
        return petMapper.toResponse(findActiveEntityById(id));
    }

    @Transactional(readOnly = true)
    public PetGroomerViewResponse findByIdGroomerView(Long id) {
        return petMapper.toGroomerViewResponse(findActiveEntityById(id));
    }

    @Transactional
    public PetResponse create(PetRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.clientId()));

        Pet pet = new Pet();
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setSize(request.size());
        pet.setNotes(request.notes());
        pet.setClient(client);

        if (request.breedId() != null) {
            Breed breed = breedRepository.findById(request.breedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed", request.breedId()));
            pet.setBreed(breed);
        }

        Pet saved = petRepository.save(pet);
        sseEmitterService.broadcastAfterCommit("pet-changed", "created", saved.getId());
        return petMapper.toResponse(saved);
    }

    @Transactional
    public PetResponse update(Long id, PetRequest request) {
        Pet pet = findEntityById(id);
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setSize(request.size());
        pet.setNotes(request.notes());

        if (!pet.getClient().getId().equals(request.clientId())) {
            Client newClient = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", request.clientId()));
            pet.setClient(newClient);
        }

        if (request.breedId() != null) {
            Breed breed = breedRepository.findById(request.breedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed", request.breedId()));
            pet.setBreed(breed);
        } else {
            pet.setBreed(null);
        }

        sseEmitterService.broadcastAfterCommit("pet-changed", "updated", id);
        return petMapper.toResponse(petRepository.save(pet));
    }

    @Transactional
    public void deactivate(Long id) {
        Pet pet = findEntityById(id);
        pet.setActive(false);
        petRepository.save(pet);
        sseEmitterService.broadcastAfterCommit("pet-changed", "deactivated", id);
    }

    private Pet findActiveEntityById(Long id) {
        return petRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }

    private Pet findEntityById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }
}
