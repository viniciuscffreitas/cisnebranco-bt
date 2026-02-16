package com.cisnebranco.event;

import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.repository.TechnicalOsRepository;
import com.cisnebranco.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class OsReadyEventListener {

    private final WhatsAppService whatsAppService;
    private final TechnicalOsRepository technicalOsRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOsReady(OsReadyEvent event) {
        try {
            TechnicalOs os = technicalOsRepository.findById(event.getOsId()).orElse(null);
            if (os == null) {
                log.warn("OS #{} not found for WhatsApp notification", event.getOsId());
                return;
            }

            String phone = os.getPet().getClient().getPhone();
            String petName = os.getPet().getName();
            String clientName = os.getPet().getClient().getName();

            log.info("OS #{} is READY — sending WhatsApp notification for pet {} to {}",
                    event.getOsId(), petName, clientName);

            whatsAppService.sendReadyNotification(phone, petName, clientName);
        } catch (Exception e) {
            log.error("Failed to send notification for OS #{}: {}", event.getOsId(), e.getMessage(), e);
        }
    }
}
