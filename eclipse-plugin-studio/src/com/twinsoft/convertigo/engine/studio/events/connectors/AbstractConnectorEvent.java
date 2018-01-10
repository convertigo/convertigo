package com.twinsoft.convertigo.engine.studio.events.connectors;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.events.AbstractEvent;

public abstract class AbstractConnectorEvent extends AbstractEvent {

    protected Connector connector;

    public AbstractConnectorEvent(String name, Connector connector) {
        super(name, connector.getQName());
        this.connector = connector;
    }
}
