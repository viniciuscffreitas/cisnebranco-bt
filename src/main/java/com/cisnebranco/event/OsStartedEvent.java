package com.cisnebranco.event;

import org.springframework.context.ApplicationEvent;

public class OsStartedEvent extends ApplicationEvent {

    private final Long osId;

    public OsStartedEvent(Object source, Long osId) {
        super(source);
        this.osId = osId;
    }

    public Long getOsId() {
        return osId;
    }
}
