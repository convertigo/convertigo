package com.twinsoft.convertigo.engine.studio.responses.connectors.events;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.connectors.AbstractConnectorResponse;

public class ConnectorEditorPartDocumentGeneratedEventResponse extends AbstractConnectorResponse {

    private String connectorOutput;

    public ConnectorEditorPartDocumentGeneratedEventResponse(Connector connector, String connectorOutput) {
        super(connector);
        this.connectorOutput = connectorOutput;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "connector_output", connectorOutput));

        return response;
    }
}
