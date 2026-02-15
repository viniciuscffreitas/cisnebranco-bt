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

    @Transactional(readOnly = true)
    public List<PetResponse> findAll() {
        return petRepository.findAll().stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetResponse> findByClient(Long clientId) {
        return petRepository.findByClientId(clientId).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetResponse findById(Long id) {
        return petMapper.toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PetGroomerViewResponse findByIdGroomerView(Long id) {
        return petMapper.toGroomerViewResponse(findEntityById(id));
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

        return petMapper.toResponse(petRepository.save(pet));
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

        return petMapper.toResponse(petRepository.save(pet));
    }

    private Pet findEntityById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }
}
