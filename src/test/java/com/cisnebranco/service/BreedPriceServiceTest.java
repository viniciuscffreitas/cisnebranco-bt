package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.response.BreedServicePriceResponse;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.entity.ServiceTypeBreedPrice;
import com.cisnebranco.entity.enums.Species;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.BreedRepository;
import com.cisnebranco.repository.ServiceTypeBreedPriceRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class BreedPriceServiceTest extends BaseIntegrationTest {

    @Autowired private BreedPriceService breedPriceService;
    @Autowired private BreedRepository breedRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private ServiceTypeBreedPriceRepository priceRepository;

    private Breed poodle;
    private ServiceType banho;
    private ServiceType inactiveService;

    @BeforeEach
    void setUp() {
        poodle = new Breed();
        poodle.setName("Poodle Teste");
        poodle.setSpecies(Species.DOG);
        poodle = breedRepository.save(poodle);

        banho = new ServiceType();
        banho.setCode("BANHO_TEST_" + System.nanoTime());
        banho.setName("Banho Teste");
        banho.setBasePrice(new BigDecimal("45.00"));
        banho.setCommissionRate(new BigDecimal("0.40"));
        banho.setDefaultDurationMinutes(30);
        banho.setActive(true);
        banho = serviceTypeRepository.save(banho);

        inactiveService = new ServiceType();
        inactiveService.setCode("TOSA_INATIVA_" + System.nanoTime());
        inactiveService.setName("Tosa Inativa");
        inactiveService.setBasePrice(new BigDecimal("80.00"));
        inactiveService.setCommissionRate(new BigDecimal("0.50"));
        inactiveService.setDefaultDurationMinutes(45);
        inactiveService.setActive(false);
        inactiveService = serviceTypeRepository.save(inactiveService);

        ServiceTypeBreedPrice banhoPrice = new ServiceTypeBreedPrice();
        banhoPrice.setServiceType(banho);
        banhoPrice.setBreed(poodle);
        banhoPrice.setPrice(new BigDecimal("55.00"));
        priceRepository.save(banhoPrice);

        ServiceTypeBreedPrice inactivePrice = new ServiceTypeBreedPrice();
        inactivePrice.setServiceType(inactiveService);
        inactivePrice.setBreed(poodle);
        inactivePrice.setPrice(new BigDecimal("99.00"));
        priceRepository.save(inactivePrice);
    }

    @Test
    void getServicePricesForBreed_returnsBreedSpecificPrice_notBasePrice() {
        List<BreedServicePriceResponse> prices = breedPriceService.getServicePricesForBreed(poodle.getId());

        assertThat(prices).hasSize(1);
        BreedServicePriceResponse response = prices.get(0);
        assertThat(response.price()).isEqualByComparingTo("55.00");
        assertThat(response.serviceTypeCode()).isEqualTo(banho.getCode());
    }

    @Test
    void getServicePricesForBreed_excludesInactiveServiceTypes() {
        List<BreedServicePriceResponse> prices = breedPriceService.getServicePricesForBreed(poodle.getId());

        assertThat(prices).noneMatch(p -> p.serviceTypeCode().equals(inactiveService.getCode()));
    }

    @Test
    void getServicePricesForBreed_unknownBreed_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> breedPriceService.getServicePricesForBreed(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getServicePricesForBreed_breedWithNoPrices_returnsEmptyList() {
        Breed noServicesBreed = new Breed();
        noServicesBreed.setName("Sem Serviços Teste");
        noServicesBreed.setSpecies(Species.CAT);
        noServicesBreed = breedRepository.save(noServicesBreed);

        List<BreedServicePriceResponse> prices = breedPriceService.getServicePricesForBreed(noServicesBreed.getId());

        assertThat(prices).isEmpty();
    }

    @Test
    void getServicePricesForBreed_responseContainsBreedId() {
        List<BreedServicePriceResponse> prices = breedPriceService.getServicePricesForBreed(poodle.getId());

        assertThat(prices).allMatch(p -> p.breedId().equals(poodle.getId()));
    }
}
