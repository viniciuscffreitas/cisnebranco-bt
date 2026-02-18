package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.AdjustServiceItemPriceRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.entity.*;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.repository.*;
import com.cisnebranco.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
class TechnicalOsServiceTest extends BaseIntegrationTest {

    @Autowired private TechnicalOsService osService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private GroomerRepository groomerRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private PricingMatrixRepository pricingMatrixRepository;
    @Autowired private InspectionPhotoRepository photoRepository;
    @Autowired private HealthChecklistRepository checklistRepository;
    @Autowired private TechnicalOsRepository osRepository;

    private Pet testPet;
    private Groomer testGroomer;
    private ServiceType banhoService;
    private ServiceType tosaTesouraService;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setName("Test Client");
        client.setPhone("11999999999");
        clientRepository.save(client);

        testPet = new Pet();
        testPet.setName("Rex");
        testPet.setSpecies(Species.DOG);
        testPet.setSize(PetSize.MEDIUM);
        testPet.setClient(client);
        petRepository.save(testPet);

        testGroomer = new Groomer();
        testGroomer.setName("Test Groomer");
        testGroomer.setPhone("11888888888");
        groomerRepository.save(testGroomer);

        // Seed data gives us service_types. Grab them by code.
        banhoService = serviceTypeRepository.findAll().stream()
                .filter(st -> "BANHO".equals(st.getCode()))
                .findFirst().orElseThrow();
        tosaTesouraService = serviceTypeRepository.findAll().stream()
                .filter(st -> "TOSA_TESOURA".equals(st.getCode()))
                .findFirst().orElseThrow();

        // BANHO DOG MEDIUM = R$50.00 (commission 40% = R$20.00)
        PricingMatrix banhoPricing = new PricingMatrix();
        banhoPricing.setServiceType(banhoService);
        banhoPricing.setSpecies(Species.DOG);
        banhoPricing.setPetSize(PetSize.MEDIUM);
        banhoPricing.setPrice(new BigDecimal("50.00"));
        pricingMatrixRepository.save(banhoPricing);

        // TOSA_TESOURA DOG MEDIUM = R$80.00 (commission 50% = R$40.00)
        PricingMatrix tosaPricing = new PricingMatrix();
        tosaPricing.setServiceType(tosaTesouraService);
        tosaPricing.setSpecies(Species.DOG);
        tosaPricing.setPetSize(PetSize.MEDIUM);
        tosaPricing.setPrice(new BigDecimal("80.00"));
        pricingMatrixRepository.save(tosaPricing);
    }

    // --- Pricing & Check-in ---

    @Test
    void checkIn_calculatesCorrectPricingAndCommission() {
        CheckInRequest request = new CheckInRequest(
                testPet.getId(), testGroomer.getId(),
                List.of(banhoService.getId(), tosaTesouraService.getId()),
                "Test notes"
        );

        TechnicalOsResponse response = osService.checkIn(request);

        // BANHO R$50 + TOSA_TESOURA R$80 = R$130
        assertThat(response.totalPrice()).isEqualByComparingTo("130.00");
        // BANHO R$50*0.40=R$20 + TOSA_TESOURA R$80*0.50=R$40 = R$60
        assertThat(response.totalCommission()).isEqualByComparingTo("60.00");
        assertThat(response.status()).isEqualTo(OsStatus.WAITING);
        assertThat(response.serviceItems()).hasSize(2);
        assertThat(response.notes()).isEqualTo("Test notes");
    }

    @Test
    void checkIn_withoutGroomer_succeeds() {
        CheckInRequest request = new CheckInRequest(
                testPet.getId(), null,
                List.of(banhoService.getId()),
                null
        );

        TechnicalOsResponse response = osService.checkIn(request);

        assertThat(response.status()).isEqualTo(OsStatus.WAITING);
        assertThat(response.groomer()).isNull();
        assertThat(response.totalPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void checkIn_noPricingFound_throws() {
        // Create a CAT pet — no pricing exists for CAT MEDIUM BANHO
        Client client = clientRepository.findAll().get(0);
        Pet cat = new Pet();
        cat.setName("Miau");
        cat.setSpecies(Species.CAT);
        cat.setSize(PetSize.SMALL);
        cat.setClient(client);
        petRepository.save(cat);

        CheckInRequest request = new CheckInRequest(
                cat.getId(), null, List.of(banhoService.getId()), null
        );

        assertThatThrownBy(() -> osService.checkIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No pricing found");
    }

    // --- State Machine Transitions ---

    @Test
    void updateStatus_waitingToInProgress_setsStartedAt() {
        Long osId = createOsInStatus(OsStatus.WAITING);

        TechnicalOsResponse result = osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.IN_PROGRESS));

        assertThat(result.status()).isEqualTo(OsStatus.IN_PROGRESS);
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    void updateStatus_invalidTransition_waitingToReady_throws() {
        Long osId = createOsInStatus(OsStatus.WAITING);

        assertThatThrownBy(() -> osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.READY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_invalidTransition_waitingToDelivered_throws() {
        Long osId = createOsInStatus(OsStatus.WAITING);

        assertThatThrownBy(() -> osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.DELIVERED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // --- READY Validation ---

    @Test
    void updateStatus_toReady_withoutPhotos_throws() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        addHealthChecklist(osId);

        assertThatThrownBy(() -> osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.READY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum 3 inspection photos");
    }

    @Test
    void updateStatus_toReady_withInsufficientPhotos_throws() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        addPhotos(osId, 2);
        addHealthChecklist(osId);

        assertThatThrownBy(() -> osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.READY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum 3 inspection photos");
    }

    @Test
    void updateStatus_toReady_withoutChecklist_throws() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        addPhotos(osId, 3);

        assertThatThrownBy(() -> osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.READY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Health checklist required");
    }

    @Test
    void updateStatus_toReady_withAllRequirements_succeeds() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        addPhotos(osId, 3);
        addHealthChecklist(osId);

        TechnicalOsResponse result = osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.READY));

        assertThat(result.status()).isEqualTo(OsStatus.READY);
        assertThat(result.finishedAt()).isNotNull();
    }

    // --- Full Lifecycle ---

    @Test
    void fullLifecycle_waitingThroughDelivered() {
        Long osId = createOsInStatus(OsStatus.WAITING);

        // WAITING → IN_PROGRESS
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.IN_PROGRESS));

        // IN_PROGRESS → READY (requires photos + checklist)
        addPhotos(osId, 3);
        addHealthChecklist(osId);
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.READY));

        // READY → DELIVERED
        TechnicalOsResponse result = osService.updateStatus(osId,
                new OsStatusUpdateRequest(OsStatus.DELIVERED));

        assertThat(result.status()).isEqualTo(OsStatus.DELIVERED);
        assertThat(result.startedAt()).isNotNull();
        assertThat(result.finishedAt()).isNotNull();
        assertThat(result.deliveredAt()).isNotNull();
    }

    // --- Access Control ---

    @Test
    void enforceAccess_adminHasFullAccess() {
        Long osId = createOsInStatus(OsStatus.WAITING);
        UserPrincipal admin = new UserPrincipal(1L, "admin", "pass",
                UserRole.ADMIN, null, true);

        assertThatCode(() -> osService.enforceAccess(osId, admin))
                .doesNotThrowAnyException();
    }

    @Test
    void enforceAccess_groomerCanAccessOwnOs() {
        Long osId = createOsWithGroomer(testGroomer);
        UserPrincipal groomer = new UserPrincipal(2L, "groomer", "pass",
                UserRole.GROOMER, testGroomer.getId(), true);

        assertThatCode(() -> osService.enforceAccess(osId, groomer))
                .doesNotThrowAnyException();
    }

    @Test
    void enforceAccess_groomerCannotAccessOthersOs() {
        Long osId = createOsWithGroomer(testGroomer);
        UserPrincipal other = new UserPrincipal(3L, "other", "pass",
                UserRole.GROOMER, 999L, true);

        assertThatThrownBy(() -> osService.enforceAccess(osId, other))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignGroomer_success() {
        Long osId = createOsInStatus(OsStatus.WAITING);
        Groomer newGroomer = new Groomer();
        newGroomer.setName("New Groomer");
        newGroomer.setPhone("11777777777");
        groomerRepository.save(newGroomer);

        TechnicalOsResponse result = osService.assignGroomer(osId, newGroomer.getId());

        assertThat(result.groomer().name()).isEqualTo("New Groomer");
    }

    // --- Price Adjustment ---

    @Test
    void adjustServiceItemPrice_happyPath_updatesTotalsAndItem() {
        TechnicalOsResponse os = osService.checkIn(new CheckInRequest(
                testPet.getId(), testGroomer.getId(),
                List.of(banhoService.getId(), tosaTesouraService.getId()), null));
        Long osId = os.id();
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.IN_PROGRESS));

        Long banhoItemId = os.serviceItems().stream()
                .filter(i -> i.serviceTypeId().equals(banhoService.getId()))
                .findFirst().orElseThrow().id();

        // Increase BANHO from R$50 to R$70 (matted coat reason)
        TechnicalOsResponse result = osService.adjustServiceItemPrice(osId, banhoItemId,
                new AdjustServiceItemPriceRequest(new BigDecimal("70.00"), "Pelagem embaraçada"));

        // BANHO R$70 + TOSA_TESOURA R$80 = R$150
        assertThat(result.totalPrice()).isEqualByComparingTo("150.00");
        // BANHO 40%*R$70=R$28 + TOSA_TESOURA 50%*R$80=R$40 = R$68
        assertThat(result.totalCommission()).isEqualByComparingTo("68.00");
        assertThat(result.serviceItems()).anySatisfy(item -> {
            assertThat(item.serviceTypeId()).isEqualTo(banhoService.getId());
            assertThat(item.lockedPrice()).isEqualByComparingTo("70.00");
            assertThat(item.commissionValue()).isEqualByComparingTo("28.00");
        });
    }

    @Test
    void adjustServiceItemPrice_priceEqualToBase_isAccepted() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        Long itemId = osService.findById(osId).serviceItems().get(0).id();

        assertThatCode(() -> osService.adjustServiceItemPrice(osId, itemId,
                new AdjustServiceItemPriceRequest(new BigDecimal("50.00"), null)))
                .doesNotThrowAnyException();
    }

    @Test
    void adjustServiceItemPrice_belowBasePrice_throws() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);
        Long itemId = osService.findById(osId).serviceItems().get(0).id();

        assertThatThrownBy(() -> osService.adjustServiceItemPrice(osId, itemId,
                new AdjustServiceItemPriceRequest(new BigDecimal("49.99"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("preço base");
    }

    @Test
    void adjustServiceItemPrice_waitingStatus_succeeds() {
        Long osId = createOsInStatus(OsStatus.WAITING);
        Long itemId = osService.findById(osId).serviceItems().get(0).id();

        TechnicalOsResponse result = osService.adjustServiceItemPrice(osId, itemId,
                new AdjustServiceItemPriceRequest(new BigDecimal("60.00"), null));

        assertThat(result.serviceItems().get(0).lockedPrice()).isEqualByComparingTo("60.00");
    }

    @Test
    void adjustServiceItemPrice_deliveredStatus_throws() {
        Long osId = createOsInStatus(OsStatus.DELIVERED);
        Long itemId = osService.findById(osId).serviceItems().get(0).id();

        assertThatThrownBy(() -> osService.adjustServiceItemPrice(osId, itemId,
                new AdjustServiceItemPriceRequest(new BigDecimal("60.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("após entrega");
    }

    @Test
    void adjustServiceItemPrice_itemFromDifferentOs_throws() {
        Long osId1 = createOsInStatus(OsStatus.IN_PROGRESS);
        Long osId2 = createOsInStatus(OsStatus.IN_PROGRESS);

        Long itemFromOs2 = osService.findById(osId2).serviceItems().get(0).id();

        assertThatThrownBy(() -> osService.adjustServiceItemPrice(osId1, itemFromOs2,
                new AdjustServiceItemPriceRequest(new BigDecimal("60.00"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adjustServiceItemPrice_nonExistentItem_throws() {
        Long osId = createOsInStatus(OsStatus.IN_PROGRESS);

        assertThatThrownBy(() -> osService.adjustServiceItemPrice(osId, 99999L,
                new AdjustServiceItemPriceRequest(new BigDecimal("60.00"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Helpers ---

    private Long createOsInStatus(OsStatus targetStatus) {
        CheckInRequest request = new CheckInRequest(
                testPet.getId(), testGroomer.getId(),
                List.of(banhoService.getId()), null
        );
        TechnicalOsResponse os = osService.checkIn(request);
        Long osId = os.id();

        // checkIn creates OS in WAITING
        if (targetStatus == OsStatus.WAITING) {
            return osId;
        }
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.IN_PROGRESS));
        if (targetStatus == OsStatus.IN_PROGRESS) {
            return osId;
        }
        addPhotos(osId, 3);
        addHealthChecklist(osId);
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.READY));
        if (targetStatus == OsStatus.READY) {
            return osId;
        }
        osService.updateStatus(osId, new OsStatusUpdateRequest(OsStatus.DELIVERED));
        if (targetStatus == OsStatus.DELIVERED) {
            return osId;
        }
        throw new IllegalArgumentException("Unsupported target status: " + targetStatus);
    }

    private Long createOsWithGroomer(Groomer groomer) {
        CheckInRequest request = new CheckInRequest(
                testPet.getId(), groomer.getId(),
                List.of(banhoService.getId()), null
        );
        return osService.checkIn(request).id();
    }

    private void addPhotos(Long osId, int count) {
        TechnicalOs os = osRepository.findById(osId).orElseThrow();
        for (int i = 0; i < count; i++) {
            InspectionPhoto photo = new InspectionPhoto();
            photo.setTechnicalOs(os);
            photo.setFilePath("/test/photo" + i + ".jpg");
            photo.setCaption("Photo " + i);
            photoRepository.save(photo);
        }
        photoRepository.flush();
    }

    private void addHealthChecklist(Long osId) {
        TechnicalOs os = osRepository.findById(osId).orElseThrow();
        HealthChecklist checklist = new HealthChecklist();
        checklist.setTechnicalOs(os);
        checklist.setSkinCondition("Normal");
        checklist.setCoatCondition("Clean");
        checklist.setHasFleas(false);
        checklist.setHasTicks(false);
        checklist.setHasWounds(false);
        os.setHealthChecklist(checklist);
        osRepository.saveAndFlush(os);
    }
}
