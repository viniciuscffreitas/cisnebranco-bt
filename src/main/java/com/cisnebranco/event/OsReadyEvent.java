package com.cisnebranco.event;

import org.springframework.context.ApplicationEvent;

public class OsReadyEvent extends ApplicationEvent {

    private final Long osId;

    public OsReadyEvent(Object source, Long osId) {
        super(source);
        this.osId = osId;
    }

    public Long getOsId() {
        return osId;
    }
}
