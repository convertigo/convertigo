package com.twinsoft.convertigo.engine.studio.events.connectors;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.responses.connectors.events.ConnectorEditorPartTransactionFinishedEventResponse;

public class ConnectorEditorPartTransactionFinishedEvent extends AbstractConnectorEvent {

    public ConnectorEditorPartTransactionFinishedEvent(Connector connector) {
        super("ConnectorEditorPart.transactionFinished", connector);
    }

    @Override
    protected Element toXml() throws Exception {
        return new ConnectorEditorPartTransactionFinishedEventResponse(connector).toXml(document, qname);
    }
}
