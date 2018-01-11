package com.twinsoft.convertigo.engine.studio.events.connectors;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.responses.connectors.events.ConnectorEditorPartDocumentGeneratedEventResponse;

public class ConnectorEditorPartDocumentGeneratedEvent extends AbstractConnectorEvent {

    private String connectorOutput;

    public ConnectorEditorPartDocumentGeneratedEvent(Connector connector, String connectorOutput) {
        super("ConnectorEditorPart.documentGenerated", connector);
        this.connectorOutput = connectorOutput;
    }

    @Override
    protected Element toXml() throws Exception {
        return new ConnectorEditorPartDocumentGeneratedEventResponse(connector, connectorOutput).toXml(document, qname);
    }
}
