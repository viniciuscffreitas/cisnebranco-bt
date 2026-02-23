package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.TechnicalOsFilterRequest;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.entity.*;
import com.cisnebranco.entity.enums.*;
import com.cisnebranco.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
class TechnicalOsFilteringTest extends BaseIntegrationTest {

    @Autowired private TechnicalOsService osService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private GroomerRepository groomerRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private PricingMatrixRepository pricingMatrixRepository;

    private Groomer groomer1;
    private Groomer groomer2;
    private Pet pet1;
    private Pet pet2;
    private ServiceType banho;

    @BeforeEach
    void setUp() {
        Client client1 = new Client();
        client1.setName("Filter Client 1");
        client1.setPhone("11999990001");
        clientRepository.save(client1);

        Client client2 = new Client();
        client2.setName("Filter Client 2");
        client2.setPhone("11999990002");
        clientRepository.save(client2);

        pet1 = new Pet();
        pet1.setName("Dog1");
        pet1.setSpecies(Species.DOG);
        pet1.setSize(PetSize.MEDIUM);
        pet1.setClient(client1);
        petRepository.save(pet1);

        pet2 = new Pet();
        pet2.setName("Dog2");
        pet2.setSpecies(Species.DOG);
        pet2.setSize(PetSize.MEDIUM);
        pet2.setClient(client2);
        petRepository.save(pet2);

        groomer1 = new Groomer();
        groomer1.setName("Filter Groomer 1");
        groomer1.setPhone("11888880001");
        groomerRepository.save(groomer1);

        groomer2 = new Groomer();
        groomer2.setName("Filter Groomer 2");
        groomer2.setPhone("11888880002");
        groomerRepository.save(groomer2);

        banho = serviceTypeRepository.findAll().stream()
                .filter(st -> "BANHO".equals(st.getCode()))
                .findFirst().orElseThrow();

        PricingMatrix pricing = new PricingMatrix();
        pricing.setServiceType(banho);
        pricing.setSpecies(Species.DOG);
        pricing.setPetSize(PetSize.MEDIUM);
        pricing.setPrice(new BigDecimal("50.00"));
        pricingMatrixRepository.save(pricing);
    }

    @Test
    void findByFilters_filterByGroomer_returnsOnlyThatGroomersOs() {
        osService.checkIn(new CheckInRequest(pet1.getId(), groomer1.getId(), List.of(banho.getId()), null, null), null);
        osService.checkIn(new CheckInRequest(pet2.getId(), groomer2.getId(), List.of(banho.getId()), null, null), null);

        Page<TechnicalOsResponse> results = osService.findByFilters(
                new TechnicalOsFilterRequest(null, groomer1.getId(), null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).groomer().id()).isEqualTo(groomer1.getId());
    }

    @Test
    void findByFilters_filterByStatus_returnsMatching() {
        osService.checkIn(new CheckInRequest(pet1.getId(), groomer1.getId(), List.of(banho.getId()), null, null), null);

        Page<TechnicalOsResponse> results = osService.findByFilters(
                new TechnicalOsFilterRequest(OsStatus.WAITING, null, null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent()).allSatisfy(os ->
                assertThat(os.status()).isEqualTo(OsStatus.WAITING));
    }

    @Test
    void findByFilters_noFilters_returnsAll() {
        osService.checkIn(new CheckInRequest(pet1.getId(), groomer1.getId(), List.of(banho.getId()), null, null), null);
        osService.checkIn(new CheckInRequest(pet2.getId(), groomer2.getId(), List.of(banho.getId()), null, null), null);

        Page<TechnicalOsResponse> results = osService.findByFilters(
                new TechnicalOsFilterRequest(null, null, null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(results.getContent().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void findByFilters_osWithoutGroomer_stillReturnedWhenNoFilter() {
        osService.checkIn(new CheckInRequest(pet1.getId(), null, List.of(banho.getId()), null, null), null);

        Page<TechnicalOsResponse> results = osService.findByFilters(
                new TechnicalOsFilterRequest(null, null, null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
    }
}
