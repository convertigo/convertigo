package com.twinsoft.convertigo.engine.studio.responses.sourcepicker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class TwsDomTreeFillDomTreeResponse extends AbstractResponse {

    private Element domTree;

    public TwsDomTreeFillDomTreeResponse(Element domTree) {
        super();
        this.domTree = domTree;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(domTree);
        return response;
    }
}
