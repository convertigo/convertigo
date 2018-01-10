package com.twinsoft.convertigo.engine.studio.editors.connectors;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;

public abstract class AbstractConnectorCompositeWrap {

    protected Connector connector;
    protected ConnectorEditorPartWrap connectorEditorPart;

    public AbstractConnectorCompositeWrap(ConnectorEditorPartWrap connectorEditorPart, Connector connector) {
        this.connectorEditorPart = connectorEditorPart;
        this.connector = connector;
    }

    protected boolean checkEventSource(EventObject event) {
        boolean isSourceFromConnector = false;
        Object source = event.getSource();
        if (event instanceof ConnectorEvent) {
            if (source instanceof DatabaseObject) {
                Connector connector = ((DatabaseObject)source).getConnector();
                if ((connector != null) && (connector.equals(this.connector)))
                    isSourceFromConnector = true;
            }
        }
        return isSourceFromConnector;
    }
}
