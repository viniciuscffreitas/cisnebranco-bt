package com.cisnebranco.event;

import org.springframework.context.ApplicationEvent;

public class OsCheckInEvent extends ApplicationEvent {

    private final Long osId;

    public OsCheckInEvent(Object source, Long osId) {
        super(source);
        this.osId = osId;
    }

    public Long getOsId() {
        return osId;
    }
}
