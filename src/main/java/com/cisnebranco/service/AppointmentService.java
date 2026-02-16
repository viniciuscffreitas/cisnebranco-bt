package com.cisnebranco.service;

import com.cisnebranco.dto.request.AppointmentRequest;
import com.cisnebranco.dto.request.AppointmentUpdateRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.response.AppointmentResponse;
import com.cisnebranco.dto.response.TimeSlot;
import com.cisnebranco.entity.Appointment;
import com.cisnebranco.entity.AvailabilityWindow;
import com.cisnebranco.entity.Client;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.Pet;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.entity.enums.AppointmentStatus;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.AppointmentMapper;
import com.cisnebranco.repository.AppointmentRepository;
import com.cisnebranco.repository.AvailabilityWindowRepository;
import com.cisnebranco.repository.ClientRepository;
import com.cisnebranco.repository.GroomerRepository;
import com.cisnebranco.repository.PetRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> VALID_TRANSITIONS = Map.of(
            AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
            AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.CANCELLED, Set.of(),
            AppointmentStatus.NO_SHOW, Set.of(),
            AppointmentStatus.COMPLETED, Set.of()
    );

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityWindowRepository windowRepository;
    private final ClientRepository clientRepository;
    private final PetRepository petRepository;
    private final GroomerRepository groomerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TechnicalOsRepository technicalOsRepository;
    private final TechnicalOsService osService;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.clientId()));

        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet", request.petId()));

        if (!pet.getClient().getId().equals(client.getId())) {
            throw new BusinessException("Pet does not belong to the specified client");
        }

        Groomer groomer = groomerRepository.findById(request.groomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", request.groomerId()));

        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", request.serviceTypeId()));

        LocalDateTime scheduledEnd = request.scheduledStart()
                .plusMinutes(serviceType.getDefaultDurationMinutes());

        // Validate appointment falls within groomer's availability window
        validateAvailabilityWindow(request.groomerId(), request.scheduledStart(), scheduledEnd);

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setPet(pet);
        appointment.setGroomer(groomer);
        appointment.setServiceType(serviceType);
        appointment.setScheduledStart(request.scheduledStart());
        appointment.setScheduledEnd(scheduledEnd);
        appointment.setNotes(request.notes());

        // The DB trigger will also reject if there's a conflict
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateAppointment(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        if (request.status() != null) {
            validateStatusTransition(appointment.getStatus(), request.status());

            if (request.status() == AppointmentStatus.CANCELLED) {
                appointment.setCancelledAt(LocalDateTime.now());
                appointment.setCancellationReason(request.cancellationReason());
            }
            appointment.setStatus(request.status());
        }

        if (request.scheduledStart() != null) {
            LocalDateTime newEnd = request.scheduledStart()
                    .plusMinutes(appointment.getServiceType().getDefaultDurationMinutes());
            validateAvailabilityWindow(appointment.getGroomer().getId(), request.scheduledStart(), newEnd);
            appointment.setScheduledStart(request.scheduledStart());
            appointment.setScheduledEnd(newEnd);
        }

        if (request.notes() != null) {
            appointment.setNotes(request.notes());
        }

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    private void validateStatusTransition(AppointmentStatus current, AppointmentStatus target) {
        Set<AppointmentStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("Invalid status transition: " + current + " → " + target);
        }
    }

    private void validateAvailabilityWindow(Long groomerId, LocalDateTime start, LocalDateTime end) {
        int dayOfWeek = start.getDayOfWeek().getValue();
        List<AvailabilityWindow> windows = windowRepository
                .findByGroomerIdAndDayOfWeekAndActiveTrue(groomerId, dayOfWeek);

        if (windows.isEmpty()) {
            throw new BusinessException("Groomer has no availability on " + start.getDayOfWeek());
        }

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        boolean withinWindow = windows.stream().anyMatch(w ->
                !startTime.isBefore(w.getStartTime()) && !endTime.isAfter(w.getEndTime()));

        if (!withinWindow) {
            throw new BusinessException("Appointment time is outside groomer's availability window");
        }
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> getAvailableSlots(Long groomerId, Long serviceTypeId, LocalDate date) {
        Groomer groomer = groomerRepository.findById(groomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", groomerId));

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));

        // ISO day of week: Monday=1, Sunday=7
        int dayOfWeek = date.getDayOfWeek().getValue();

        List<AvailabilityWindow> windows = windowRepository
                .findByGroomerIdAndDayOfWeekAndActiveTrue(groomerId, dayOfWeek);

        if (windows.isEmpty()) {
            return List.of();
        }

        int durationMinutes = serviceType.getDefaultDurationMinutes();

        // Get existing appointments for this date
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Appointment> existingAppointments = appointmentRepository
                .findActiveByGroomerAndDateRange(groomerId, dayStart, dayEnd);

        List<TimeSlot> slots = new ArrayList<>();

        for (AvailabilityWindow window : windows) {
            LocalTime slotStart = window.getStartTime();
            while (slotStart.plusMinutes(durationMinutes).compareTo(window.getEndTime()) <= 0) {
                LocalDateTime start = date.atTime(slotStart);
                LocalDateTime end = start.plusMinutes(durationMinutes);

                boolean available = existingAppointments.stream().noneMatch(a ->
                        a.getScheduledStart().isBefore(end) && a.getScheduledEnd().isAfter(start));

                slots.add(new TimeSlot(start, end, available));
                slotStart = slotStart.plusMinutes(durationMinutes);
            }
        }

        return slots;
    }

    @Transactional
    public AppointmentResponse convertToOs(Long appointmentId, CheckInRequest checkInRequest) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (appointment.getTechnicalOs() != null) {
            throw new BusinessException("Appointment already converted to OS");
        }

        var osResponse = osService.checkIn(checkInRequest);

        // Link the OS to the appointment using a managed reference
        var os = technicalOsRepository.getReferenceById(osResponse.id());
        appointment.setTechnicalOs(os);
        appointment.setStatus(AppointmentStatus.COMPLETED);

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return appointmentRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }
        return appointmentRepository.findByClientId(clientId)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }
}
