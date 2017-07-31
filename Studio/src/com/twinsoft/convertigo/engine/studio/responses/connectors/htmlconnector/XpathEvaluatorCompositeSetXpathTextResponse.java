package com.twinsoft.convertigo.engine.studio.responses.connectors.htmlconnector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class XpathEvaluatorCompositeSetXpathTextResponse extends AbstractResponse {

    private String xpath;

    public XpathEvaluatorCompositeSetXpathTextResponse(String xpath) {
        this.xpath = xpath;
    }
    
    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "xpath", xpath));
        return response;
    }
}
