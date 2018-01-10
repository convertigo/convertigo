package com.twinsoft.convertigo.engine.studio.responses.connectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public abstract class AbstractConnectorResponse extends AbstractResponse {
   
    protected Connector connector;

    public AbstractConnectorResponse(Connector connector) {
        this.connector = connector;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);

        response.setAttribute("project", connector.getProject().getName());
        response.setAttribute("connector", connector.getName());
        response.setAttribute("type_editor", getTypeConnectorEditor());

        return response;
    }

    private String getTypeConnectorEditor() {
        return "c8o_" + connector.getClass().getSimpleName().toLowerCase() + "_editor";
    }
}
