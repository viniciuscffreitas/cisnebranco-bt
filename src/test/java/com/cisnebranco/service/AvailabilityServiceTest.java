package com.cisnebranco.service;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.dto.request.AvailabilityWindowRequest;
import com.cisnebranco.dto.response.AvailabilityWindowResponse;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.GroomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
class AvailabilityServiceTest extends BaseIntegrationTest {

    @Autowired private AvailabilityService availabilityService;
    @Autowired private GroomerRepository groomerRepository;

    private Groomer groomer;

    @BeforeEach
    void setUp() {
        groomer = new Groomer();
        groomer.setName("Availability Groomer");
        groomer.setPhone("11777770000");
        groomerRepository.save(groomer);
    }

    @Test
    void createWindow_validRequest_succeeds() {
        AvailabilityWindowResponse response = availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        assertThat(response.dayOfWeek()).isEqualTo(1);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void createWindow_endBeforeStart_throws() {
        assertThatThrownBy(() -> availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(17, 0), LocalTime.of(9, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void createWindow_overlappingWindow_throws() {
        availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        assertThatThrownBy(() -> availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(12, 0), LocalTime.of(17, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void createWindow_nonOverlappingOnSameDay_succeeds() {
        availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        AvailabilityWindowResponse response = availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(13, 0), LocalTime.of(17, 0)));

        assertThat(response.startTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void createWindow_sameTimesDifferentDays_succeeds() {
        availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        AvailabilityWindowResponse response = availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(2, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        assertThat(response.dayOfWeek()).isEqualTo(2);
    }

    @Test
    void createWindow_invalidGroomerId_throws() {
        assertThatThrownBy(() -> availabilityService.createWindow(99999L,
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGroomerAvailability_returnsActiveWindows() {
        availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(3, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        List<AvailabilityWindowResponse> windows = availabilityService.getGroomerAvailability(groomer.getId());

        assertThat(windows).hasSize(2);
    }

    @Test
    void deleteWindow_softDeletes() {
        AvailabilityWindowResponse created = availabilityService.createWindow(groomer.getId(),
                new AvailabilityWindowRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        availabilityService.deleteWindow(created.id());

        List<AvailabilityWindowResponse> windows = availabilityService.getGroomerAvailability(groomer.getId());
        assertThat(windows).isEmpty();
    }

    @Test
    void deleteWindow_invalidId_throws() {
        assertThatThrownBy(() -> availabilityService.deleteWindow(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
