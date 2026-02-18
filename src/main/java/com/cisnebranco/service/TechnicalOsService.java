package com.cisnebranco.service;

import com.cisnebranco.dto.request.AdjustServiceItemPriceRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
import com.cisnebranco.dto.request.TechnicalOsFilterRequest;
import com.cisnebranco.dto.response.TechnicalOsGroomerViewResponse;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.specification.TechnicalOsSpecification;
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
import com.cisnebranco.repository.OsServiceItemRepository;
import com.cisnebranco.repository.PetRepository;
import com.cisnebranco.repository.PricingMatrixRepository;
import com.cisnebranco.repository.ServiceTypeRepository;
import com.cisnebranco.repository.TechnicalOsRepository;
import com.cisnebranco.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final OsServiceItemRepository serviceItemRepository;
    private final TechnicalOsMapper osMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public TechnicalOsResponse checkIn(CheckInRequest request) {
        Pet pet = petRepository.findByIdAndActiveTrue(request.petId())
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
            ServiceType serviceType = serviceTypeRepository.findByIdAndActiveTrue(serviceTypeId)
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
        var response = osMapper.toResponse(osRepository.save(os));
        auditService.log("STATUS_CHANGED", "TechnicalOs", osId, currentStatus + " → " + newStatus);

        String petName = os.getPet().getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sseEmitterService.sendToAll("os-status-changed", Map.of(
                            "osId", osId,
                            "status", newStatus.name(),
                            "previousStatus", currentStatus.name(),
                            "petName", petName
                    ));
                } catch (Exception e) {
                    log.warn("Failed to broadcast SSE event for OS {}", osId, e);
                }
            }
        });

        return response;
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
    public Page<TechnicalOsResponse> findAll(Pageable pageable) {
        return osRepository.findAllWithDetails(pageable)
                .map(osMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TechnicalOsResponse> findByFilters(TechnicalOsFilterRequest filter, Pageable pageable) {
        Specification<TechnicalOs> spec = Specification
                .where(TechnicalOsSpecification.hasStatus(filter.status()))
                .and(TechnicalOsSpecification.hasGroomer(filter.groomerId()))
                .and(TechnicalOsSpecification.hasClient(filter.clientId()))
                .and(TechnicalOsSpecification.hasPaymentStatus(filter.paymentStatus()))
                .and(TechnicalOsSpecification.createdBetween(filter.startDate(), filter.endDate()));

        return osRepository.findAll(spec, pageable).map(osMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TechnicalOsResponse findById(Long id) {
        return osMapper.toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public TechnicalOsGroomerViewResponse findByIdForGroomer(Long id, Long groomerId) {
        TechnicalOs os = findEntityById(id);
        if (os.getGroomer() == null || !os.getGroomer().getId().equals(groomerId)) {
            throw new AccessDeniedException("Groomer can only view their own service orders");
        }
        return osMapper.toGroomerViewResponse(os);
    }

    @Transactional(readOnly = true)
    public Page<TechnicalOsGroomerViewResponse> findByGroomer(Long groomerId, Pageable pageable) {
        return osRepository.findByGroomerIdWithDetails(groomerId, pageable)
                .map(osMapper::toGroomerViewResponse);
    }

    @Transactional(readOnly = true)
    public void enforceAccess(Long osId, UserPrincipal principal) {
        if (principal.getRole() == UserRole.GROOMER) {
            TechnicalOs os = findEntityById(osId);
            if (os.getGroomer() == null || !os.getGroomer().getId().equals(principal.getGroomerId())) {
                throw new AccessDeniedException("Groomer can only manage their own service orders");
            }
        }
    }

    @Transactional
    public TechnicalOsResponse adjustServiceItemPrice(Long osId, Long itemId, AdjustServiceItemPriceRequest request) {
        TechnicalOs os = findEntityById(osId);

        OsServiceItem item = serviceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("OsServiceItem", itemId));

        if (!item.getTechnicalOs().getId().equals(osId)) {
            throw new BusinessException("Service item does not belong to OS #" + osId);
        }

        if (request.adjustedPrice().compareTo(item.getLockedPrice()) < 0) {
            throw new BusinessException("Adjusted price cannot be lower than the base price (R$ "
                    + item.getLockedPrice() + ")");
        }

        BigDecimal newCommission = request.adjustedPrice()
                .multiply(item.getLockedCommissionRate())
                .setScale(2, RoundingMode.HALF_UP);

        item.setLockedPrice(request.adjustedPrice());
        item.setCommissionValue(newCommission);

        // Recalculate totals from all items to avoid lost-update on concurrent calls
        BigDecimal newTotal = os.getServiceItems().stream()
                .map(OsServiceItem::getLockedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newTotalCommission = os.getServiceItems().stream()
                .map(OsServiceItem::getCommissionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        os.setTotalPrice(newTotal);
        os.setTotalCommission(newTotalCommission);

        auditService.log("PRICE_ADJUSTED", "OsServiceItem", itemId,
                "OS #" + osId + " item adjusted to R$ " + request.adjustedPrice());

        return osMapper.toResponse(osRepository.save(os));
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
