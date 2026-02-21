package com.cisnebranco.service;

import com.cisnebranco.dto.request.ClientFilterRequest;
import com.cisnebranco.dto.request.ClientRequest;
import com.cisnebranco.dto.response.ClientResponse;
import com.cisnebranco.entity.Client;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.ClientMapper;
import com.cisnebranco.repository.ClientRepository;
import com.cisnebranco.specification.ClientSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    // Placeholder used when anonymizing PII — satisfies the NOT NULL DB constraint on phone.
    // "0" becomes "550" after formatPhone(), which is < 12 chars and will be rejected by
    // WhatsAppService before any API call is made, so no bogus notifications are sent.
    private static final String ANONYMIZED_PHONE = "0";

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final SseEmitterService sseEmitterService;
    private final AuditService auditService;

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
    public Page<ClientResponse> findByFilters(ClientFilterRequest filter, Pageable pageable) {
        Specification<Client> spec = Specification
                .where(ClientSpecification.nameContains(filter.name()))
                .and(ClientSpecification.phoneContains(filter.phone()))
                .and(ClientSpecification.registeredAfter(filter.registeredAfter()));

        return clientRepository.findAll(spec, pageable).map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = clientMapper.toEntity(request);
        Client saved = clientRepository.save(client);
        sseEmitterService.broadcastAfterCommit("client-changed", "created", saved.getId());
        return clientMapper.toResponse(saved);
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findEntityById(id);
        clientMapper.updateEntity(request, client);
        sseEmitterService.broadcastAfterCommit("client-changed", "updated", id);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    /**
     * LGPD Art. 18 — Right to erasure.
     *
     * Hard delete is blocked because the client may have historical OS records that
     * must be retained for financial and audit purposes. Instead, all PII is
     * anonymized in-place, leaving the shell record for referential integrity.
     * An audit entry is written so the anonymization event is traceable.
     */
    @Transactional
    public void delete(Long id) {
        Client client = findEntityById(id);
        client.setName("CLIENTE EXCLUÍDO #" + id);
        client.setPhone(ANONYMIZED_PHONE);
        client.setEmail(null);
        client.setAddress(null);
        clientRepository.save(client);
        // logOrThrow: if the audit write fails, the outer @Transactional rolls back the
        // anonymization — preserving the PII until the audit infrastructure is healthy.
        auditService.logOrThrow("CLIENT_ANONYMIZED", "Client", id,
                "Dados pessoais anonimizados por solicitação (LGPD Art. 18)");
        log.info("Client #{} anonymized (LGPD)", id);
        sseEmitterService.broadcastAfterCommit("client-changed", "deleted", id);
    }

    private Client findEntityById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }
}
