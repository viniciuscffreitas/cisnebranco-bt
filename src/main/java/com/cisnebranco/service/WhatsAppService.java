package com.cisnebranco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    private final RestClient restClient;
    private final String instanceName;
    private final boolean enabled;

    public WhatsAppService(
            @Value("${app.whatsapp.evolution-api-url}") String baseUrl,
            @Value("${app.whatsapp.evolution-api-key}") String apiKey,
            @Value("${app.whatsapp.instance-name}") String instanceName,
            @Value("${app.whatsapp.enabled}") boolean enabled) {
        this.instanceName = instanceName;
        this.enabled = enabled;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    public void sendReadyNotification(String phone, String petName, String clientName) {
        if (!enabled) {
            log.debug("WhatsApp disabled — skipping notification for {}", petName);
            return;
        }

        String number = formatPhone(phone);
        String message = """
                Olá, %s! 🐾
                O banho e tosa do(a) %s já está pronto(a).
                Você já pode vir buscá-lo(a) no Cisne Branco!"""
                .formatted(clientName, petName);

        try {
            restClient.post()
                    .uri("/message/sendText/{instance}", instanceName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "number", number,
                            "text", message
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp notification sent to {} for pet {}", number, petName);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification to {}: {}", number, e.getMessage());
        }
    }

    private String formatPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }
        return digits;
    }
}
