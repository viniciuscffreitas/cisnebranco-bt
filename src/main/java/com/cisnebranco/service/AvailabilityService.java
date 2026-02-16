package com.cisnebranco.service;

import com.cisnebranco.dto.request.AvailabilityWindowRequest;
import com.cisnebranco.dto.response.AvailabilityWindowResponse;
import com.cisnebranco.entity.AvailabilityWindow;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.AvailabilityWindowMapper;
import com.cisnebranco.repository.AvailabilityWindowRepository;
import com.cisnebranco.repository.GroomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityWindowRepository windowRepository;
    private final GroomerRepository groomerRepository;
    private final AvailabilityWindowMapper windowMapper;

    @Transactional
    public AvailabilityWindowResponse createWindow(Long groomerId, AvailabilityWindowRequest request) {
        Groomer groomer = groomerRepository.findById(groomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", groomerId));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }

        AvailabilityWindow window = new AvailabilityWindow();
        window.setGroomer(groomer);
        window.setDayOfWeek(request.dayOfWeek());
        window.setStartTime(request.startTime());
        window.setEndTime(request.endTime());

        return windowMapper.toResponse(windowRepository.save(window));
    }

    @Transactional(readOnly = true)
    public List<AvailabilityWindowResponse> getGroomerAvailability(Long groomerId) {
        if (!groomerRepository.existsById(groomerId)) {
            throw new ResourceNotFoundException("Groomer", groomerId);
        }
        return windowRepository.findByGroomerIdAndActiveTrue(groomerId)
                .stream()
                .map(windowMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteWindow(Long windowId) {
        AvailabilityWindow window = windowRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("AvailabilityWindow", windowId));
        window.setActive(false);
        windowRepository.save(window);
    }
}
