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
public class OsStartedEventListener {

    private final WhatsAppService whatsAppService;
    private final TechnicalOsRepository technicalOsRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOsStarted(OsStartedEvent event) {
        try {
            TechnicalOs os = technicalOsRepository.findByIdWithPetAndClient(event.getOsId())
                    .orElse(null);
            if (os == null) {
                log.warn("OS #{} not found for in-progress WhatsApp notification", event.getOsId());
                return;
            }

            if (os.getPet() == null || os.getPet().getClient() == null) {
                log.warn("OS #{} has incomplete data (missing pet or client), skipping in-progress notification",
                        event.getOsId());
                return;
            }

            String phone = os.getPet().getClient().getPhone();
            String petName = os.getPet().getName();
            String clientName = os.getPet().getClient().getName();

            log.info("OS #{} started — sending WhatsApp notification for pet {} to {}",
                    event.getOsId(), petName, clientName);

            whatsAppService.sendInProgressNotification(phone, petName, clientName);
        } catch (Exception e) {
            log.error("Failed to send in-progress WhatsApp notification for OS #{}: {}",
                    event.getOsId(), e.getMessage(), e);
        }
    }
}
