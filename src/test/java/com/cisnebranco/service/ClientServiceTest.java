package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.ClientFilterRequest;
import com.cisnebranco.dto.request.ClientRequest;
import com.cisnebranco.dto.response.ClientResponse;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@Transactional
class ClientServiceTest extends BaseIntegrationTest {

    @Autowired private ClientService clientService;
    @Autowired private ClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        clientService.create(new ClientRequest("Maria Silva", "11999990001", "maria@email.com", "Rua A"));
        clientService.create(new ClientRequest("João Santos", "11999990002", null, "Rua B"));
        clientService.create(new ClientRequest("Ana Oliveira", "21888880003", "ana@email.com", null));
    }

    @Test
    void create_validClient_succeeds() {
        ClientResponse response = clientService.create(
                new ClientRequest("New Client", "11777770000", "new@email.com", "Rua C"));

        assertThat(response.name()).isEqualTo("New Client");
        assertThat(response.phone()).isEqualTo("11777770000");
        assertThat(response.id()).isNotNull();
    }

    @Test
    void findById_existingClient_succeeds() {
        ClientResponse created = clientService.create(
                new ClientRequest("Find Me", "11666660000", null, null));

        ClientResponse found = clientService.findById(created.id());

        assertThat(found.name()).isEqualTo("Find Me");
    }

    @Test
    void findById_nonExistent_throws() {
        assertThatThrownBy(() -> clientService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_existingClient_succeeds() {
        ClientResponse created = clientService.create(
                new ClientRequest("Old Name", "11555550000", null, null));

        ClientResponse updated = clientService.update(created.id(),
                new ClientRequest("New Name", "11555550000", "new@email.com", "New Address"));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.email()).isEqualTo("new@email.com");
    }

    @Test
    void findByFilters_nameFilter_findsMatching() {
        Page<ClientResponse> results = clientService.findByFilters(
                new ClientFilterRequest("silva", null, null), PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).name()).isEqualTo("Maria Silva");
    }

    @Test
    void findByFilters_phoneFilter_findsMatching() {
        Page<ClientResponse> results = clientService.findByFilters(
                new ClientFilterRequest(null, "21888", null), PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).name()).isEqualTo("Ana Oliveira");
    }

    @Test
    void findByFilters_noFilters_returnsAll() {
        Page<ClientResponse> results = clientService.findByFilters(
                new ClientFilterRequest(null, null, null), PageRequest.of(0, 10));

        assertThat(results.getContent().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void findByFilters_noMatch_returnsEmpty() {
        Page<ClientResponse> results = clientService.findByFilters(
                new ClientFilterRequest("nonexistent", null, null), PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
    }
}
