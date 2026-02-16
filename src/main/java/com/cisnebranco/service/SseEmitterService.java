package com.cisnebranco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SseEmitterService {

    private static final long EMITTER_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            log.info("SSE completed for user: {}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE timeout for user: {}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onError(e -> {
            log.warn("SSE error for user: {}", userId, e);
            emitters.remove(userId, emitter);
        });

        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            old.complete();
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notifications stream"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event for user {}, removing emitter", userId, e);
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            trySend(userId, emitter, eventName, data);
        }
    }

    public void sendToAll(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> trySend(userId, emitter, eventName, data));
    }

    private void trySend(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("Failed to send SSE event '{}' to user {}, removing emitter", eventName, userId, e);
            emitters.remove(userId, emitter);
        }
    }

    @Scheduled(fixedRate = 30_000)
    void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("Heartbeat failed for user {}, removing emitter", userId);
                emitters.remove(userId, emitter);
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
