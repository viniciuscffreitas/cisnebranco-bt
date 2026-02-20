package com.cisnebranco.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    private final RestClient restClient;
    private final String instanceName;
    private final String apiKey;
    private final boolean enabled;

    public WhatsAppService(
            @Value("${app.whatsapp.evolution-api-url}") String baseUrl,
            @Value("${app.whatsapp.evolution-api-key}") String apiKey,
            @Value("${app.whatsapp.instance-name}") String instanceName,
            @Value("${app.whatsapp.enabled}") boolean enabled) {
        this.instanceName = instanceName;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    @PostConstruct
    void validateConfiguration() {
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException(
                    "WhatsApp is enabled but EVOLUTION_API_KEY is not set. " +
                    "Either set the API key or disable WhatsApp with WHATSAPP_ENABLED=false.");
        }
        if (enabled) {
            log.info("WhatsApp notifications enabled — instance: {}", instanceName);
        }
    }

    public void sendReadyNotification(String phone, String petName, String clientName, BigDecimal balance) {
        if (!enabled) {
            log.debug("WhatsApp disabled — skipping notification for {}", petName);
            return;
        }

        if (phone == null || phone.isBlank()) {
            log.warn("WhatsApp skipped: phone is null for client {}", clientName);
            return;
        }

        String number = formatPhone(phone);
        if (number.length() < 12) {
            log.warn("Invalid phone for client {} (pet {}), skipping WhatsApp notification",
                    clientName, petName);
            return;
        }

        String message;
        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            String balanceFormatted = String.format(new Locale("pt", "BR"), "R$ %.2f", balance);
            message = """
                    Olá, %s! 🐾
                    O banho e tosa do(a) %s já está pronto(a).
                    Saldo a pagar: %s.
                    Aguardamos você no Cisne Branco!"""
                    .formatted(clientName, petName, balanceFormatted);
        } else {
            message = """
                    Olá, %s! 🐾
                    O banho e tosa do(a) %s já está pronto(a).
                    Você já pode vir buscá-lo(a) no Cisne Branco!"""
                    .formatted(clientName, petName);
        }

        restClient.post()
                .uri("/message/sendText/{instance}", instanceName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "number", number,
                        "text", message
                ))
                .retrieve()
                .toBodilessEntity();

        log.info("WhatsApp notification sent to {} for pet {}", maskPhone(number), petName);
    }

    private String formatPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }
        return digits;
    }

    private String maskPhone(String number) {
        if (number.length() <= 4) return "****";
        return number.substring(0, 4) + "****" + number.substring(number.length() - 2);
    }
}
