package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.PaymentRequest;
import com.cisnebranco.dto.response.PaymentEventResponse;
import com.cisnebranco.entity.*;
import com.cisnebranco.entity.enums.*;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
class PaymentServiceTest extends BaseIntegrationTest {

    @Autowired private PaymentService paymentService;
    @Autowired private TechnicalOsService osService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private GroomerRepository groomerRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private PricingMatrixRepository pricingMatrixRepository;
    @Autowired private AppUserRepository userRepository;
    @Autowired private TechnicalOsRepository osRepository;
    @Autowired private EntityManager entityManager;

    private Long osId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setName("Payment Test Client");
        client.setPhone("11999990000");
        clientRepository.save(client);

        Pet pet = new Pet();
        pet.setName("Buddy");
        pet.setSpecies(Species.DOG);
        pet.setSize(PetSize.MEDIUM);
        pet.setClient(client);
        petRepository.save(pet);

        Groomer groomer = new Groomer();
        groomer.setName("Groomer Pay");
        groomer.setPhone("11888880000");
        groomerRepository.save(groomer);

        ServiceType banho = serviceTypeRepository.findAll().stream()
                .filter(st -> "BANHO".equals(st.getCode()))
                .findFirst().orElseThrow();

        PricingMatrix pricing = new PricingMatrix();
        pricing.setServiceType(banho);
        pricing.setSpecies(Species.DOG);
        pricing.setPetSize(PetSize.MEDIUM);
        pricing.setPrice(new BigDecimal("100.00"));
        pricingMatrixRepository.save(pricing);

        var osResponse = osService.checkIn(new CheckInRequest(
                pet.getId(), groomer.getId(), List.of(banho.getId()), null, null), null);
        osId = osResponse.id();

        AppUser user = new AppUser();
        user.setUsername("paytest");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void recordPayment_partialPayment_succeeds() {
        PaymentEventResponse response = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("40.00"), PaymentMethod.PIX, "TX001", "Partial"), userId);

        assertThat(response.amount()).isEqualByComparingTo("40.00");
        assertThat(response.method()).isEqualTo(PaymentMethod.PIX);
        assertThat(response.transactionRef()).isEqualTo("TX001");
    }

    @Test
    void recordPayment_fullPayment_succeeds() {
        PaymentEventResponse response = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD, null, null), userId);

        assertThat(response.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void recordPayment_exceedsBalance_throws() {
        assertThatThrownBy(() -> paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("150.00"), PaymentMethod.PIX, null, null), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds remaining balance");
    }

    @Test
    void recordPayment_multiplePartials_succeedUntilFull() {
        paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("30.00"), PaymentMethod.PIX, null, null), userId);
        paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("30.00"), PaymentMethod.CASH, null, null), userId);
        paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("40.00"), PaymentMethod.DEBIT_CARD, null, null), userId);

        // Now fully paid — any extra should fail
        assertThatThrownBy(() -> paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("0.01"), PaymentMethod.PIX, null, null), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds remaining balance");
    }

    @Test
    void recordPayment_invalidOsId_throws() {
        assertThatThrownBy(() -> paymentService.recordPayment(99999L,
                new PaymentRequest(new BigDecimal("10.00"), PaymentMethod.PIX, null, null), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordPayment_invalidUserId_throws() {
        assertThatThrownBy(() -> paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("10.00"), PaymentMethod.PIX, null, null), 99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refundPayment_validRefund_succeeds() {
        PaymentEventResponse payment = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, "TX-REF", null), userId);

        PaymentEventResponse refund = paymentService.refundPayment(osId, payment.id(), userId);

        assertThat(refund.amount()).isEqualByComparingTo("-50.00");
        assertThat(refund.notes()).contains("Refund of payment");
    }

    @Test
    void refundPayment_doubleRefund_throws() {
        PaymentEventResponse payment = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, null, null), userId);

        paymentService.refundPayment(osId, payment.id(), userId);

        assertThatThrownBy(() -> paymentService.refundPayment(osId, payment.id(), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been refunded");
    }

    @Test
    void refundPayment_wrongOsId_throws() {
        PaymentEventResponse payment = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, null, null), userId);

        // Service validates event belongs to OS before looking up the OS
        assertThatThrownBy(() -> paymentService.refundPayment(99999L, payment.id(), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to the specified OS");
    }

    @Test
    void refundPayment_eventFromDifferentOs_throws() {
        PaymentEventResponse payment = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, null, null), userId);

        // Create another OS using a different pet size to avoid pricing unique constraint
        Client client = clientRepository.findAll().get(0);
        Pet pet2 = new Pet();
        pet2.setName("Rex2");
        pet2.setSpecies(Species.DOG);
        pet2.setSize(PetSize.LARGE);
        pet2.setClient(client);
        petRepository.save(pet2);

        ServiceType banho = serviceTypeRepository.findAll().stream()
                .filter(st -> "BANHO".equals(st.getCode())).findFirst().orElseThrow();
        PricingMatrix pricing = new PricingMatrix();
        pricing.setServiceType(banho);
        pricing.setSpecies(Species.DOG);
        pricing.setPetSize(PetSize.LARGE);
        pricing.setPrice(new BigDecimal("120.00"));
        pricingMatrixRepository.save(pricing);

        var os2 = osService.checkIn(new CheckInRequest(pet2.getId(), null, List.of(banho.getId()), null, null), null);

        assertThatThrownBy(() -> paymentService.refundPayment(os2.id(), payment.id(), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to the specified OS");
    }

    @Test
    void getPaymentHistory_returnsAllEvents() {
        paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("30.00"), PaymentMethod.PIX, null, "First"), userId);
        paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("20.00"), PaymentMethod.CASH, null, "Second"), userId);

        List<PaymentEventResponse> history = paymentService.getPaymentHistory(osId);

        assertThat(history).hasSize(2);
    }

    @Test
    void getPaymentHistory_invalidOsId_throws() {
        assertThatThrownBy(() -> paymentService.getPaymentHistory(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordPayment_onCancelledOs_throws() {
        // payment_status is updatable=false (managed by trigger), so use native SQL
        entityManager.createNativeQuery(
                "UPDATE technical_os SET payment_status = 'CANCELLED' WHERE id = :id")
                .setParameter("id", osId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        PaymentRequest request = new PaymentRequest(
                new BigDecimal("10.00"), PaymentMethod.PIX, null, null);

        assertThatThrownBy(() -> paymentService.recordPayment(osId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void refundPayment_onDeliveredOs_throws() {
        PaymentEventResponse payment = paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, null, null), userId);

        entityManager.createNativeQuery(
                "UPDATE technical_os SET status = 'DELIVERED' WHERE id = :id")
                .setParameter("id", osId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> paymentService.refundPayment(osId, payment.id(), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivered");
    }

    @Test
    void recordPayment_onReadyOs_succeeds() {
        // READY is the state where payment is collected before pet handoff — must remain allowed
        entityManager.createNativeQuery(
                "UPDATE technical_os SET status = 'READY' WHERE id = :id")
                .setParameter("id", osId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThatCode(() -> paymentService.recordPayment(osId,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.PIX, null, null), userId))
                .doesNotThrowAnyException();
    }

    @Test
    void recordPayment_onDeliveredOs_throws() {
        entityManager.createNativeQuery(
                "UPDATE technical_os SET status = 'DELIVERED' WHERE id = :id")
                .setParameter("id", osId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        PaymentRequest request = new PaymentRequest(
                new BigDecimal("10.00"), PaymentMethod.PIX, null, null);

        assertThatThrownBy(() -> paymentService.recordPayment(osId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivered");
    }
}
