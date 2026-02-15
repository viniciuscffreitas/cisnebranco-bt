package com.cisnebranco.service;

import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
import com.cisnebranco.dto.response.TechnicalOsGroomerViewResponse;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.OsServiceItem;
import com.cisnebranco.entity.Pet;
import com.cisnebranco.entity.PricingMatrix;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.event.OsReadyEvent;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.TechnicalOsMapper;
import com.cisnebranco.repository.GroomerRepository;
import com.cisnebranco.repository.InspectionPhotoRepository;
import com.cisnebranco.repository.PetRepository;
import com.cisnebranco.repository.PricingMatrixRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import com.cisnebranco.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TechnicalOsService {

    private static final Map<OsStatus, OsStatus> VALID_TRANSITIONS = Map.of(
            OsStatus.SCHEDULED, OsStatus.WAITING,
            OsStatus.WAITING, OsStatus.IN_PROGRESS,
            OsStatus.IN_PROGRESS, OsStatus.READY,
            OsStatus.READY, OsStatus.DELIVERED
    );

    private final TechnicalOsRepository osRepository;
    private final PetRepository petRepository;
    private final GroomerRepository groomerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PricingMatrixRepository pricingMatrixRepository;
    private final InspectionPhotoRepository photoRepository;
    private final TechnicalOsMapper osMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TechnicalOsResponse checkIn(CheckInRequest request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet", request.petId()));

        TechnicalOs os = new TechnicalOs();
        os.setPet(pet);
        os.setStatus(OsStatus.WAITING);
        os.setNotes(request.notes());

        if (request.groomerId() != null) {
            Groomer groomer = groomerRepository.findById(request.groomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Groomer", request.groomerId()));
            os.setGroomer(groomer);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (Long serviceTypeId : request.serviceTypeIds()) {
            ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));

            PricingMatrix pricing = pricingMatrixRepository
                    .findByServiceTypeIdAndSpeciesAndPetSize(serviceTypeId, pet.getSpecies(), pet.getSize())
                    .orElseThrow(() -> new BusinessException(
                            "No pricing found for service " + serviceType.getName()
                            + " / " + pet.getSpecies() + " / " + pet.getSize()));

            BigDecimal commissionValue = pricing.getPrice()
                    .multiply(serviceType.getCommissionRate())
                    .setScale(2, RoundingMode.HALF_UP);

            OsServiceItem item = new OsServiceItem();
            item.setTechnicalOs(os);
            item.setServiceType(serviceType);
            item.setLockedPrice(pricing.getPrice());
            item.setLockedCommissionRate(serviceType.getCommissionRate());
            item.setCommissionValue(commissionValue);

            os.getServiceItems().add(item);
            totalPrice = totalPrice.add(pricing.getPrice());
            totalCommission = totalCommission.add(commissionValue);
        }

        os.setTotalPrice(totalPrice);
        os.setTotalCommission(totalCommission);

        TechnicalOs saved = osRepository.save(os);
        return osMapper.toResponse(saved);
    }

    @Transactional
    public TechnicalOsResponse updateStatus(Long osId, OsStatusUpdateRequest request) {
        TechnicalOs os = findEntityById(osId);
        OsStatus newStatus = request.status();
        OsStatus currentStatus = os.getStatus();

        OsStatus expectedNext = VALID_TRANSITIONS.get(currentStatus);
        if (expectedNext == null || expectedNext != newStatus) {
            throw new BusinessException("Invalid status transition: " + currentStatus + " → " + newStatus);
        }

        if (newStatus == OsStatus.READY) {
            validateReadyRequirements(os);
        }

        switch (newStatus) {
            case IN_PROGRESS -> os.setStartedAt(LocalDateTime.now());
            case READY -> {
                os.setFinishedAt(LocalDateTime.now());
                eventPublisher.publishEvent(new OsReadyEvent(this, osId));
            }
            case DELIVERED -> os.setDeliveredAt(LocalDateTime.now());
            default -> {}
        }

        os.setStatus(newStatus);
        return osMapper.toResponse(osRepository.save(os));
    }

    @Transactional
    public TechnicalOsResponse assignGroomer(Long osId, Long groomerId) {
        TechnicalOs os = findEntityById(osId);
        Groomer groomer = groomerRepository.findById(groomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", groomerId));
        os.setGroomer(groomer);
        return osMapper.toResponse(osRepository.save(os));
    }

    @Transactional(readOnly = true)
    public List<TechnicalOsResponse> findAll() {
        return osRepository.findAllWithDetails().stream()
                .map(osMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TechnicalOsResponse findById(Long id) {
        return osMapper.toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<TechnicalOsGroomerViewResponse> findByGroomer(Long groomerId) {
        return osRepository.findByGroomerIdWithDetails(groomerId).stream()
                .map(osMapper::toGroomerViewResponse)
                .toList();
    }

    public void enforceAccess(Long osId, UserPrincipal principal) {
        if (principal.getRole() == UserRole.GROOMER) {
            TechnicalOs os = findEntityById(osId);
            if (os.getGroomer() == null || !os.getGroomer().getId().equals(principal.getGroomerId())) {
                throw new BusinessException("Groomer can only manage their own service orders");
            }
        }
    }

    private void validateReadyRequirements(TechnicalOs os) {
        long photoCount = photoRepository.countByTechnicalOsId(os.getId());
        if (photoCount < 3) {
            throw new BusinessException("Minimum 3 inspection photos required before marking READY (current: " + photoCount + ")");
        }
        if (os.getHealthChecklist() == null) {
            throw new BusinessException("Health checklist required before marking READY");
        }
    }

    private TechnicalOs findEntityById(Long id) {
        return osRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", id));
    }
}
