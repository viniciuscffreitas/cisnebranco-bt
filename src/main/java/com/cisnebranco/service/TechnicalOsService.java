package com.cisnebranco.service;

import com.cisnebranco.dto.request.AdjustServiceItemPriceRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.request.OsStatusUpdateRequest;
import com.cisnebranco.dto.request.TechnicalOsFilterRequest;
import com.cisnebranco.dto.response.AuditLogResponse;
import com.cisnebranco.dto.response.TechnicalOsGroomerViewResponse;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.specification.TechnicalOsSpecification;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.OsServiceItem;
import com.cisnebranco.entity.Pet;
import com.cisnebranco.entity.PricingMatrix;
import com.cisnebranco.entity.ServiceType;
import com.cisnebranco.entity.ServiceTypeBreedPrice;
import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.event.OsCheckInEvent;
import com.cisnebranco.event.OsReadyEvent;
import com.cisnebranco.event.OsStartedEvent;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.mapper.TechnicalOsMapper;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.PaymentEvent;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.GroomerRepository;
import jakarta.persistence.EntityManager;
import com.cisnebranco.repository.InspectionPhotoRepository;
import com.cisnebranco.repository.PaymentEventRepository;
import com.cisnebranco.repository.PetRepository;
import com.cisnebranco.repository.PricingMatrixRepository;
import com.cisnebranco.repository.ServiceTypeBreedPriceRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalOsService {

    private static final Set<OsStatus> GROOMER_ASSIGNABLE_STATUSES =
            EnumSet.of(OsStatus.SCHEDULED, OsStatus.WAITING);

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
    private final ServiceTypeBreedPriceRepository breedPriceRepository;
    private final InspectionPhotoRepository photoRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final AppUserRepository appUserRepository;
    private final EntityManager entityManager;
    private final TechnicalOsMapper osMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public TechnicalOsResponse checkIn(CheckInRequest request, Long userId) {
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

            BigDecimal lockedPrice = resolveLockedPrice(serviceTypeId, serviceType, pet);
            BigDecimal commissionValue = lockedPrice
                    .multiply(serviceType.getCommissionRate())
                    .setScale(2, RoundingMode.HALF_UP);

            OsServiceItem item = new OsServiceItem();
            item.setTechnicalOs(os);
            item.setServiceType(serviceType);
            item.setLockedPrice(lockedPrice);
            item.setLockedCommissionRate(serviceType.getCommissionRate());
            item.setCommissionValue(commissionValue);

            os.getServiceItems().add(item);
            totalPrice = totalPrice.add(lockedPrice);
            totalCommission = totalCommission.add(commissionValue);
        }

        os.setTotalPrice(totalPrice);
        os.setTotalCommission(totalCommission);

        TechnicalOs saved = osRepository.save(os);

        if (request.prepaidPayment() != null) {
            recordPrepaidPayment(saved, request.prepaidPayment(), userId);
        }

        // Audit only after all mutations succeed so a BusinessException in
        // recordPrepaidPayment rolls back the OS without leaving a phantom audit entry.
        auditService.log("CHECKIN", "TechnicalOs", saved.getId(),
                "Check-in realizado para o pet #" + pet.getId());

        // Flush pending SQL then refresh to pick up DB-trigger-computed columns
        // (payment_status, payment_balance) before building the response.
        osRepository.flush();
        entityManager.refresh(saved);

        eventPublisher.publishEvent(new OsCheckInEvent(this, saved.getId()));
        return osMapper.toResponse(saved);
    }

    private void recordPrepaidPayment(TechnicalOs os, CheckInRequest.PrepaidPaymentRequest prepaid, Long userId) {
        Objects.requireNonNull(userId, "userId must not be null when recording prepaid payment");

        if (os.getTotalPrice().compareTo(BigDecimal.ZERO) == 0) {
            log.error("OS #{} has totalPrice=ZERO — possible pricing matrix misconfiguration", os.getId());
            throw new BusinessException("Valor total da OS é zero. Verifique a configuração de preços.");
        }
        if (prepaid.amount().compareTo(os.getTotalPrice()) > 0) {
            log.warn("Prepaid amount {} exceeds OS total {} for pet #{} (userId={})",
                    prepaid.amount().toPlainString(), os.getTotalPrice().toPlainString(),
                    os.getPet().getId(), userId);
            throw new BusinessException(
                    "Pagamento antecipado (R$ " + prepaid.amount().toPlainString() +
                    ") não pode exceder o valor total da OS (R$ " + os.getTotalPrice().toPlainString() + ")");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        PaymentEvent event = new PaymentEvent();
        event.setTechnicalOs(os);
        event.setAmount(prepaid.amount());
        event.setMethod(prepaid.method());
        event.setTransactionRef(prepaid.transactionRef());
        event.setNotes("Pagamento antecipado registrado no check-in");
        event.setCreatedBy(user);
        paymentEventRepository.save(event);

        // Additive update — correct even if totalPaid was non-zero (defensive).
        os.setTotalPaid(os.getTotalPaid().add(prepaid.amount()));
        osRepository.save(os);

        // logOrThrow: payment is a financial record — silent audit loss is a compliance gap.
        auditService.logOrThrow("PAYMENT_RECORDED", "TechnicalOs", os.getId(),
                "Prepaid: R$ " + prepaid.amount().toPlainString() + " via " + prepaid.method());
        log.info("Prepaid payment of {} recorded for OS #{}", prepaid.amount(), os.getId());
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
            case READY -> os.setFinishedAt(LocalDateTime.now());
            case DELIVERED -> os.setDeliveredAt(LocalDateTime.now());
            default -> {}
        }

        os.setStatus(newStatus);
        var response = osMapper.toResponse(osRepository.save(os));

        // Publish events after save so all listeners read committed data via AFTER_COMMIT.
        if (newStatus == OsStatus.READY) {
            eventPublisher.publishEvent(new OsReadyEvent(this, osId));
        }
        auditService.log("STATUS_CHANGED", "TechnicalOs", osId, currentStatus + " → " + newStatus);

        if (newStatus == OsStatus.IN_PROGRESS) {
            auditService.log("INICIO_SERVICO", "TechnicalOs", osId,
                    "Groomer iniciou o atendimento da OS #" + osId);
            eventPublisher.publishEvent(new OsStartedEvent(this, osId));
        } else if (newStatus == OsStatus.READY) {
            auditService.log("SERVICO_CONCLUIDO", "TechnicalOs", osId,
                    "Groomer concluiu o atendimento da OS #" + osId);
        }

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
                    // SSE broadcast is best-effort; the transaction has already committed.
                    // Failure here means connected clients won't receive the real-time update
                    // but data is consistent in the DB.
                    log.error("Failed to broadcast SSE event for OS {}", osId, e);
                }
            }
        });

        return response;
    }

    @Transactional
    public TechnicalOsResponse assignGroomer(Long osId, Long groomerId) {
        TechnicalOs os = findEntityById(osId);

        if (!GROOMER_ASSIGNABLE_STATUSES.contains(os.getStatus())) {
            throw new BusinessException(
                    "Groomer reassignment is only allowed in SCHEDULED or WAITING status (current: "
                    + os.getStatus() + ")");
        }

        Long previousGroomerId = os.getGroomer() != null ? os.getGroomer().getId() : null;

        // No-op guard — avoids spurious audit entries and SSE events
        if (groomerId.equals(previousGroomerId)) {
            return osMapper.toResponse(os);
        }

        Groomer groomer = groomerRepository.findById(groomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Groomer", groomerId));
        if (!groomer.isActive()) {
            throw new BusinessException("Cannot assign groomer #" + groomerId + " — groomer is inactive");
        }

        os.setGroomer(groomer);
        TechnicalOsResponse response = osMapper.toResponse(osRepository.save(os));

        log.info("Groomer reassigned on OS #{}: {} -> #{}", osId, previousGroomerId, groomerId);

        // [I2] auditService uses REQUIRES_NEW — it commits independently of the outer transaction.
        // If the outer transaction rolls back after this line, the audit entry will remain.
        // Accepted: audit over-recording is preferable to audit under-recording.
        auditService.log("GROOMER_REASSIGNED", "TechnicalOs", osId,
                "Groomer changed from #" + previousGroomerId + " to #" + groomerId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sseEmitterService.sendToAll("groomer-assigned", Map.of(
                            "osId", osId,
                            "groomerId", groomerId
                    ));
                } catch (Exception e) {
                    log.error("Failed to broadcast SSE event for groomer assignment on OS {} (groomerId={})",
                            osId, groomerId, e);
                }
            }
        });

        return response;
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
        // Pessimistic write lock prevents concurrent price updates from producing stale totals
        TechnicalOs os = osRepository.findByIdForUpdate(osId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalOs", osId));

        // [C2] Guard: price adjustment is blocked once the service is complete (READY fires the
        // client notification; DELIVERED is the terminal state). Only WAITING and IN_PROGRESS permit it.
        if (os.getStatus() == OsStatus.READY || os.getStatus() == OsStatus.DELIVERED) {
            throw new BusinessException(
                    "Ajuste de preço não é permitido após conclusão do serviço (status atual: " + os.getStatus() + ")");
        }

        // Find item within the OS collection — mutating in-place ensures the
        // subsequent total recalculation stream sees the updated lockedPrice
        OsServiceItem item = os.getServiceItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Attempted to adjust item {} which does not belong to OS {}", itemId, osId);
                    return new AccessDeniedException("Service item does not belong to OS #" + osId);
                });

        if (item.getLockedPrice() == null) {
            log.error("OS #{} item #{} has null lockedPrice — data integrity issue", osId, itemId);
            throw new BusinessException("Item #" + itemId + " tem preço base inválido. Contate o suporte.");
        }

        if (request.adjustedPrice().compareTo(item.getLockedPrice()) < 0) {
            throw new BusinessException("O preço ajustado não pode ser menor que o preço base (R$ "
                    + item.getLockedPrice() + ")");
        }

        BigDecimal maxAllowedPrice = item.getLockedPrice().multiply(new BigDecimal("3"));
        if (request.adjustedPrice().compareTo(maxAllowedPrice) > 0) {
            throw new BusinessException("O preço ajustado não pode exceder 3× o valor base (máx. R$ "
                    + maxAllowedPrice.setScale(2, RoundingMode.HALF_UP) + ")");
        }

        BigDecimal originalPrice = item.getLockedPrice();
        BigDecimal newCommission = request.adjustedPrice()
                .multiply(item.getLockedCommissionRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Mutate in-place — Hibernate dirty-checking cascades the save via CascadeType.ALL
        item.setLockedPrice(request.adjustedPrice());
        item.setCommissionValue(newCommission);

        // Recalculate from the (now-mutated) collection — no stale reference possible
        BigDecimal newTotal = os.getServiceItems().stream()
                .map(OsServiceItem::getLockedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newTotalCommission = os.getServiceItems().stream()
                .map(OsServiceItem::getCommissionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        os.setTotalPrice(newTotal);
        os.setTotalCommission(newTotalCommission);

        // [I2] Save first, then audit — REQUIRES_NEW in AuditService would otherwise
        // commit an audit entry even if the save rolls back
        TechnicalOsResponse response = osMapper.toResponse(osRepository.save(os));

        String auditDetail = "OS #" + osId + " item #" + itemId
                + " adjusted from R$ " + originalPrice + " to R$ " + request.adjustedPrice()
                + (request.reason() != null && !request.reason().isBlank()
                        ? " — motivo: " + request.reason() : "");
        auditService.log("PRICE_ADJUSTED", "OsServiceItem", itemId, auditDetail);

        // [C3] SSE broadcast after commit — consistent with updateStatus pattern
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sseEmitterService.sendToAll("os-price-adjusted", Map.of(
                            "osId", osId,
                            "itemId", itemId,
                            "adjustedPrice", request.adjustedPrice().toPlainString(),
                            "totalPrice", response.totalPrice().toPlainString()
                    ));
                } catch (Exception e) {
                    // SSE broadcast is best-effort; the transaction has already committed.
                    // Failure here means connected clients won't receive the real-time update
                    // but data is consistent in the DB.
                    log.error("Failed to broadcast SSE event for price adjustment on OS {} item {}", osId, itemId, e);
                }
            }
        });

        return response;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getOsAuditLog(Long osId) {
        findEntityById(osId); // verify OS exists before returning audit entries
        return auditService.findByOs(osId);
    }

    private BigDecimal resolveLockedPrice(Long serviceTypeId, ServiceType serviceType, Pet pet) {
        if (pet.getBreed() != null) {
            var breedPrice = breedPriceRepository
                    .findByServiceTypeIdAndBreedId(serviceTypeId, pet.getBreed().getId());
            if (breedPrice.isPresent()) {
                return breedPrice.get().getPrice();
            }
        }
        return pricingMatrixRepository
                .findByServiceTypeIdAndSpeciesAndPetSize(serviceTypeId, pet.getSpecies(), pet.getSize())
                .map(PricingMatrix::getPrice)
                .orElseThrow(() -> new BusinessException(
                        "No pricing found for service " + serviceType.getName()
                        + " / " + pet.getSpecies() + " / " + pet.getSize()));
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
