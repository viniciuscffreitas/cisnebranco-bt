package com.cisnebranco.event;

import com.cisnebranco.entity.OsServiceItem;
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

import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OsCheckInEventListener {

    private final WhatsAppService whatsAppService;
    private final TechnicalOsRepository technicalOsRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOsCheckIn(OsCheckInEvent event) {
        try {
            TechnicalOs os = technicalOsRepository.findByIdWithPetClientAndServices(event.getOsId())
                    .orElse(null);
            if (os == null) {
                log.warn("OS #{} not found for check-in WhatsApp notification", event.getOsId());
                return;
            }

            if (os.getPet() == null || os.getPet().getClient() == null) {
                log.warn("OS #{} has incomplete data (missing pet or client), skipping check-in notification",
                        event.getOsId());
                return;
            }

            String phone = os.getPet().getClient().getPhone();
            String petName = os.getPet().getName();
            String clientName = os.getPet().getClient().getName();
            String serviceName = os.getServiceItems().stream()
                    .map(OsServiceItem::getServiceType)
                    .filter(st -> st != null && st.getName() != null)
                    .map(st -> st.getName())
                    .collect(Collectors.joining(", "));
            if (serviceName.isBlank()) serviceName = "serviço(s) agendado(s)";

            log.info("OS #{} checked in — sending WhatsApp notification for pet {} to {}",
                    event.getOsId(), petName, clientName);

            whatsAppService.sendCheckInNotification(phone, petName, clientName, serviceName);
        } catch (Exception e) {
            log.error("Failed to send check-in WhatsApp notification for OS #{}: {}",
                    event.getOsId(), e.getMessage(), e);
        }
    }
}
