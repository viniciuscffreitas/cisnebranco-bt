package com.cisnebranco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onError(e -> {
            log.debug("SSE error for user: {}", userId);
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notifications stream"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event", e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE to user {}, removing emitter", userId);
                emitters.remove(userId);
            }
        }
    }

    public void sendToAll(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE to user {}, removing emitter", userId);
                emitters.remove(userId);
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
