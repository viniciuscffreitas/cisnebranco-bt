package com.cisnebranco.service;

import com.cisnebranco.dto.response.WeeklyCommissionResponse;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.WeeklyCommission;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.WeeklyCommissionMapper;
import com.cisnebranco.repository.GroomerRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import com.cisnebranco.repository.WeeklyCommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyCommissionService {

    private final WeeklyCommissionRepository commissionRepository;
    private final TechnicalOsRepository osRepository;
    private final GroomerRepository groomerRepository;
    private final WeeklyCommissionMapper commissionMapper;

    @Transactional
    public WeeklyCommissionResponse generateForWeek(Long groomerId, LocalDate weekStart) {
        Groomer groomer = groomerRepository.findById(groomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", groomerId));

        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        List<TechnicalOs> deliveredOs = osRepository
                .findDeliveredByGroomerAndDateRange(groomerId, start, end);

        BigDecimal totalRevenue = deliveredOs.stream()
                .map(TechnicalOs::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = deliveredOs.stream()
                .map(TechnicalOs::getTotalCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WeeklyCommission commission = commissionRepository
                .findByGroomerIdAndWeekStart(groomerId, weekStart)
                .orElseGet(WeeklyCommission::new);

        commission.setGroomer(groomer);
        commission.setWeekStart(weekStart);
        commission.setWeekEnd(weekEnd);
        commission.setTotalServices(deliveredOs.size());
        commission.setTotalRevenue(totalRevenue);
        commission.setTotalCommission(totalCommission);

        return commissionMapper.toResponse(commissionRepository.save(commission));
    }

    @Transactional(readOnly = true)
    public Page<WeeklyCommissionResponse> findByGroomer(Long groomerId, Pageable pageable) {
        return commissionRepository.findByGroomerId(groomerId, pageable)
                .map(commissionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WeeklyCommissionResponse> findAll(Pageable pageable) {
        return commissionRepository.findAll(pageable)
                .map(commissionMapper::toResponse);
    }
}
