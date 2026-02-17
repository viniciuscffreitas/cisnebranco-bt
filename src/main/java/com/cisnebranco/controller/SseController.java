package com.cisnebranco.controller;

import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSE", description = "Server-Sent Events for real-time notifications")
public class SseController {

    private final SseEmitterService sseEmitterService;

    @Operation(summary = "Stream real-time notifications")
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        boolean isReconnect = lastEventId != null && !lastEventId.isBlank();
        if (isReconnect) {
            String safeLastEventId = lastEventId.length() > 20 ? lastEventId.substring(0, 20) + "..." : lastEventId;
            log.info("SSE reconnection for user: {} (id: {}), last event: {}", principal.getUsername(), principal.getId(), safeLastEventId);
        } else {
            log.info("SSE connection opened for user: {} (id: {})", principal.getUsername(), principal.getId());
        }
        return sseEmitterService.createEmitter(principal.getId(), isReconnect);
    }
}
