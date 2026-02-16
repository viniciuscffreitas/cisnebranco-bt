package com.cisnebranco.service;

import com.cisnebranco.dto.request.ClientRequest;
import com.cisnebranco.dto.response.ClientResponse;
import com.cisnebranco.entity.Client;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.ClientMapper;
import com.cisnebranco.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public Page<ClientResponse> findAll(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String name, Pageable pageable) {
        return clientRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = clientMapper.toEntity(request);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findEntityById(id);
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public void delete(Long id) {
        Client client = findEntityById(id);
        clientRepository.delete(client);
    }

    private Client findEntityById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }
}
