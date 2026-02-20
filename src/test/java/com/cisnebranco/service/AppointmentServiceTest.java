package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.AppointmentRequest;
import com.cisnebranco.dto.request.AppointmentUpdateRequest;
import com.cisnebranco.dto.request.AvailabilityWindowRequest;
import com.cisnebranco.dto.response.AppointmentResponse;
import com.cisnebranco.dto.response.TimeSlot;
import com.cisnebranco.entity.*;
import com.cisnebranco.entity.enums.*;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.*;
import com.cisnebranco.entity.Breed;
import com.cisnebranco.entity.ServiceTypeBreedPrice;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
class AppointmentServiceTest extends BaseIntegrationTest {

    @Autowired private AppointmentService appointmentService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private GroomerRepository groomerRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private AvailabilityWindowRepository windowRepository;
    @Autowired private BreedRepository breedRepository;
    @Autowired private ServiceTypeBreedPriceRepository breedPriceRepository;
    @Autowired private PricingMatrixRepository pricingMatrixRepository;

    private Client client;
    private Pet pet;
    private Groomer groomer;
    private ServiceType serviceType;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setName("Appointment Client");
        client.setPhone("11999991111");
        clientRepository.save(client);

        pet = new Pet();
        pet.setName("Luna");
        pet.setSpecies(Species.DOG);
        pet.setSize(PetSize.SMALL);
        pet.setClient(client);
        petRepository.save(pet);

        groomer = new Groomer();
        groomer.setName("Appointment Groomer");
        groomer.setPhone("11888881111");
        groomerRepository.save(groomer);

        serviceType = serviceTypeRepository.findAll().stream()
                .filter(st -> "BANHO".equals(st.getCode()))
                .findFirst().orElseThrow();

        // Create availability windows for the groomer (Mon-Fri 8:00-18:00)
        for (int day = 1; day <= 5; day++) {
            AvailabilityWindow window = new AvailabilityWindow();
            window.setGroomer(groomer);
            window.setDayOfWeek(day);
            window.setStartTime(LocalTime.of(8, 0));
            window.setEndTime(LocalTime.of(18, 0));
            windowRepository.save(window);
        }
    }

    @Test
    void createAppointment_validRequest_succeeds() {
        // Next Monday at 10:00
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);

        AppointmentResponse response = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, "Test appointment"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.scheduledStart()).isEqualTo(nextMonday);
        assertThat(response.scheduledEnd()).isEqualTo(nextMonday.plusMinutes(serviceType.getDefaultDurationMinutes()));
        assertThat(response.notes()).isEqualTo("Test appointment");
    }

    @Test
    void createAppointment_petNotBelongToClient_throws() {
        Client otherClient = new Client();
        otherClient.setName("Other Client");
        otherClient.setPhone("11999992222");
        clientRepository.save(otherClient);

        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);

        assertThatThrownBy(() -> appointmentService.createAppointment(
                new AppointmentRequest(otherClient.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pet does not belong");
    }

    @Test
    void createAppointment_outsideAvailabilityWindow_throws() {
        // Saturday (day 6) — groomer has no availability
        LocalDateTime saturday = getNextWeekday(6).atTime(10, 0);

        assertThatThrownBy(() -> appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), saturday, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no availability");
    }

    @Test
    void createAppointment_outsideWindowHours_throws() {
        // Monday at 7:00 — before 8:00 window start
        LocalDateTime earlyMonday = getNextWeekday(1).atTime(7, 0);

        assertThatThrownBy(() -> appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), earlyMonday, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outside groomer's availability");
    }

    @Test
    void createAppointment_invalidClientId_throws() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);

        assertThatThrownBy(() -> appointmentService.createAppointment(
                new AppointmentRequest(99999L, pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppointment_cancelValid_succeeds() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        AppointmentResponse created = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        AppointmentResponse cancelled = appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.CANCELLED, null, "Client requested"));

        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void updateAppointment_invalidTransition_scheduledToCompleted_throws() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        AppointmentResponse created = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        assertThatThrownBy(() -> appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.COMPLETED, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateAppointment_confirmThenComplete_succeeds() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        AppointmentResponse created = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.CONFIRMED, null, null));

        AppointmentResponse completed = appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.COMPLETED, null, null));

        assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void updateAppointment_cancelledCannotTransition_throws() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        AppointmentResponse created = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.CANCELLED, null, "test"));

        assertThatThrownBy(() -> appointmentService.updateAppointment(created.id(),
                new AppointmentUpdateRequest(null, AppointmentStatus.CONFIRMED, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void getAvailableSlots_returnsCorrectSlots() {
        LocalDate nextMonday = getNextWeekday(1);
        List<TimeSlot> slots = appointmentService.getAvailableSlots(
                groomer.getId(), serviceType.getId(), nextMonday);

        assertThat(slots).isNotEmpty();
        assertThat(slots).allSatisfy(slot -> {
            assertThat(slot.start().toLocalDate()).isEqualTo(nextMonday);
            assertThat(slot.end()).isAfter(slot.start());
        });
    }

    @Test
    void getAvailableSlots_withExistingAppointment_marksSlotUnavailable() {
        LocalDate nextMonday = getNextWeekday(1);
        LocalDateTime appointmentTime = nextMonday.atTime(10, 0);

        appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), appointmentTime, null));

        List<TimeSlot> slots = appointmentService.getAvailableSlots(
                groomer.getId(), serviceType.getId(), nextMonday);

        // The slot at 10:00 should not be available
        TimeSlot bookedSlot = slots.stream()
                .filter(s -> s.start().equals(appointmentTime))
                .findFirst().orElseThrow();
        assertThat(bookedSlot.available()).isFalse();
    }

    @Test
    void getAvailableSlots_noAvailabilityOnDay_returnsEmpty() {
        LocalDate saturday = getNextWeekday(6);
        List<TimeSlot> slots = appointmentService.getAvailableSlots(
                groomer.getId(), serviceType.getId(), saturday);

        assertThat(slots).isEmpty();
    }

    @Test
    void findByDateRange_returnsAppointmentsInRange() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        List<AppointmentResponse> results = appointmentService.findByDateRange(
                nextMonday.minusHours(1), nextMonday.plusHours(1));

        assertThat(results).hasSize(1);
    }

    @Test
    void findByClient_returnsClientAppointments() {
        LocalDateTime nextMonday = getNextWeekday(1).atTime(10, 0);
        appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), nextMonday, null));

        List<AppointmentResponse> results = appointmentService.findByClient(client.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).client().id()).isEqualTo(client.getId());
    }

    @Test
    void findByClient_invalidClientId_throws() {
        assertThatThrownBy(() -> appointmentService.findByClient(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Estimated Price ---

    @Test
    void createAppointment_petWithBreedAndBreedPrice_estimatedPriceIsBreedPrice() {
        Breed poodle = new Breed();
        poodle.setName("Poodle Appt Test " + System.nanoTime());
        poodle.setSpecies(Species.DOG);
        poodle = breedRepository.save(poodle);

        ServiceTypeBreedPrice breedPrice = new ServiceTypeBreedPrice();
        breedPrice.setServiceType(serviceType);
        breedPrice.setBreed(poodle);
        breedPrice.setPrice(new BigDecimal("65.00"));
        breedPriceRepository.save(breedPrice);

        pet.setBreed(poodle);
        petRepository.save(pet);

        AppointmentResponse response = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), getNextWeekday(1).atTime(10, 0), null));

        assertThat(response.estimatedPrice()).isEqualByComparingTo("65.00");
    }

    @Test
    void createAppointment_petWithBreedButNoBreedPrice_estimatedPriceFallsBackToMatrix() {
        Breed labrador = new Breed();
        labrador.setName("Labrador Appt Test " + System.nanoTime());
        labrador.setSpecies(Species.DOG);
        labrador = breedRepository.save(labrador);

        pet.setBreed(labrador);
        petRepository.save(pet);

        PricingMatrix matrix = new PricingMatrix();
        matrix.setServiceType(serviceType);
        matrix.setSpecies(Species.DOG);
        matrix.setPetSize(PetSize.SMALL);
        matrix.setPrice(new BigDecimal("40.00"));
        pricingMatrixRepository.save(matrix);

        AppointmentResponse response = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), getNextWeekday(1).atTime(10, 0), null));

        assertThat(response.estimatedPrice()).isEqualByComparingTo("40.00");
    }

    @Test
    void createAppointment_petWithNoBreedAndNoPricingMatrix_estimatedPriceIsNull() {
        // pet has no breed and no PricingMatrix configured — estimatedPrice must be null
        AppointmentResponse response = appointmentService.createAppointment(
                new AppointmentRequest(client.getId(), pet.getId(), groomer.getId(),
                        serviceType.getId(), getNextWeekday(1).atTime(10, 0), null));

        assertThat(response.estimatedPrice()).isNull();
    }

    private LocalDate getNextWeekday(int isoDayOfWeek) {
        LocalDate today = LocalDate.now().plusDays(7); // ensure future
        while (today.getDayOfWeek().getValue() != isoDayOfWeek) {
            today = today.plusDays(1);
        }
        return today;
    }
}
