package com.cisnebranco.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OsReadyEventListener {

    @EventListener
    public void handleOsReady(OsReadyEvent event) {
        // Phase 2: WhatsApp notification via Evolution API
        log.info("OS #{} is READY — WhatsApp notification placeholder", event.getOsId());
    }
}
