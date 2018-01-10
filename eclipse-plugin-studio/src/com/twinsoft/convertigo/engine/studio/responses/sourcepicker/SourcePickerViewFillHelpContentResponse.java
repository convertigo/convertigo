package com.twinsoft.convertigo.engine.studio.responses.sourcepicker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class SourcePickerViewFillHelpContentResponse extends AbstractResponse {

    private String tag;
    private String type;
    private String name;
    private String comment;
    private String textBtn;
    private boolean enableBtn;

    public SourcePickerViewFillHelpContentResponse(String tag, String type, String name, String comment, String textShowBtn, boolean enableBtn) {
        super();
        this.tag = tag;
        this.type = type;
        this.name = name;
        this.comment = comment;
        this.textBtn = textShowBtn;
        this.enableBtn = enableBtn;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "tag", tag));
        response.appendChild(DOMUtils.createElementWithText(document, "type", type));
        response.appendChild(DOMUtils.createElementWithText(document, "name", name));
        response.appendChild(DOMUtils.createElementWithText(document, "comment", comment));
        response.appendChild(DOMUtils.createElementWithText(document, "text_show_btn", textBtn));
        response.appendChild(DOMUtils.createElementWithText(document, "enable_btn", Boolean.toString(enableBtn)));

        return response;
    }
}
