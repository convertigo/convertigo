package com.twinsoft.convertigo.engine.studio.responses.connectors.htmlconnector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class XpathEvaluatorCompositeSetXpathTextResponse extends AbstractResponse {

    private String xpath;
    private String anchor;

    public XpathEvaluatorCompositeSetXpathTextResponse(String xpath, String anchor) {
        this.xpath = xpath;
        this.anchor = anchor;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "xpath", xpath));
        response.appendChild(DOMUtils.createElementWithText(document, "anchor", anchor));
        return response;
    }
}
