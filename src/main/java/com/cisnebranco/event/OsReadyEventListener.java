package com.cisnebranco.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OsReadyEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOsReady(OsReadyEvent event) {
        try {
            // Phase 2: WhatsApp notification via Evolution API
            log.info("OS #{} is READY — WhatsApp notification placeholder", event.getOsId());
        } catch (Exception e) {
            log.error("Failed to send notification for OS #{}: {}", event.getOsId(), e.getMessage(), e);
        }
    }
}
