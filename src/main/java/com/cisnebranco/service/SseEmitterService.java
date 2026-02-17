package com.cisnebranco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class SseEmitterService {

    private static final long EMITTER_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong eventIdCounter = new AtomicLong(0);

    public SseEmitter createEmitter(Long userId, boolean isReconnect) {
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
            try {
                old.complete();
            } catch (Exception e) {
                log.debug("Could not complete previous emitter for user {} during replacement: {}", userId, e.getMessage());
            }
        }

        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(eventIdCounter.incrementAndGet()))
                    .name("connected")
                    .data(Map.of("reconnect", isReconnect)));
        } catch (IOException | IllegalStateException e) {
            log.warn("Failed to send initial connected event for user {}, aborting emitter registration: {}", userId, e.getMessage());
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        } catch (Exception e) {
            log.error("Unexpected error sending initial connected event for user {}, aborting emitter registration", userId, e);
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE event '{}' not delivered to user {} — no active connection", eventName, userId);
            return;
        }
        trySend(userId, emitter, eventName, data);
    }

    public void sendToAll(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> trySend(userId, emitter, eventName, data));
    }

    private void trySend(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(eventIdCounter.incrementAndGet()))
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("SSE send failed (IO) for event '{}' to user {}, removing emitter: {}", eventName, userId, e.getMessage());
            emitters.remove(userId, emitter);
        } catch (IllegalStateException e) {
            log.warn("SSE send failed (emitter already closed) for event '{}' to user {}, removing emitter: {}", eventName, userId, e.getMessage());
            emitters.remove(userId, emitter);
        } catch (Exception e) {
            log.error("Unexpected error sending SSE event '{}' to user {}, completing emitter with error", eventName, userId, e);
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }
    }

    @Scheduled(fixedRate = 30_000)
    void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.warn("SSE heartbeat failed (IO) for user {}, removing emitter: {}", userId, e.getMessage());
                emitters.remove(userId, emitter);
            } catch (IllegalStateException e) {
                log.warn("SSE heartbeat failed (emitter already closed) for user {}, removing emitter: {}", userId, e.getMessage());
                emitters.remove(userId, emitter);
            } catch (Exception e) {
                log.error("Unexpected error during SSE heartbeat for user {}, completing emitter with error", userId, e);
                emitters.remove(userId, emitter);
                emitter.completeWithError(e);
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
