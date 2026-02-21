package com.cisnebranco.service;

import com.cisnebranco.dto.response.AuditLogResponse;
import com.cisnebranco.entity.AuditLog;
import com.cisnebranco.repository.AuditLogRepository;
import com.cisnebranco.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String details) {
        persistEntry(action, entityType, entityId, details, getCurrentUsername());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String details, String username) {
        persistEntry(action, entityType, entityId, details, username);
    }

    /**
     * Like {@link #log}, but does NOT suppress exceptions. Use for irreversible operations
     * (e.g. LGPD erasure) where a missing audit entry is a compliance failure — the caller's
     * transaction will roll back if the audit write fails, keeping data consistent.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrThrow(String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setUsername(getCurrentUsername());
        entry.setIpAddress(getCurrentIp());
        auditLogRepository.save(entry);
        log.debug("Audit (critical): {} {} #{} — {}", action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByOs(Long osId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtAsc("TechnicalOs", osId)
                .stream()
                .map(e -> new AuditLogResponse(
                        e.getId(), e.getAction(), e.getEntityType(), e.getEntityId(),
                        e.getUsername(), e.getDetails(), e.getIpAddress(), e.getCreatedAt()))
                .toList();
    }

    private void persistEntry(String action, String entityType, Long entityId, String details, String username) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            entry.setUsername(username);
            entry.setIpAddress(getCurrentIp());

            auditLogRepository.save(entry);
            log.debug("Audit: {} {} #{} by {} - {}", action, entityType, entityId, username, details);
        } catch (Exception e) {
            // [AUDIT_FAILURE] Intentionally non-fatal — the business operation has already committed.
            // Alert on this log pattern to detect audit gaps before they become a compliance issue.
            log.error("[AUDIT_FAILURE] action={} entity={}#{} user={} — audit entry lost",
                    action, entityType, entityId, username, e);
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        if (auth != null) {
            log.warn("SecurityContext principal is not UserPrincipal: type={}", auth.getPrincipal().getClass().getSimpleName());
        }
        return "system";
    }

    private String getCurrentIp() {
        var requestAttrs = RequestContextHolder.getRequestAttributes();
        if (requestAttrs == null) {
            return null;
        }
        if (!(requestAttrs instanceof ServletRequestAttributes servletAttrs)) {
            log.warn("RequestAttributes is not ServletRequestAttributes: {}", requestAttrs.getClass().getSimpleName());
            return null;
        }
        var request = servletAttrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
