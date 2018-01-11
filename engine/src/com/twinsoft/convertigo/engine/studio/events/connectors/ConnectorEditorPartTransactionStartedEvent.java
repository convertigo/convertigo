package com.twinsoft.convertigo.engine.studio.events.connectors;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.responses.connectors.events.ConnectorEditorPartTransactionStartedEventResponse;

public class ConnectorEditorPartTransactionStartedEvent extends AbstractConnectorEvent {

    public ConnectorEditorPartTransactionStartedEvent(Connector connector) {
        super("ConnectorEditorPart.transactionStarted", connector);
    }

    @Override
    protected Element toXml() throws Exception {
        return new ConnectorEditorPartTransactionStartedEventResponse(connector).toXml(document, qname);
    }
}
