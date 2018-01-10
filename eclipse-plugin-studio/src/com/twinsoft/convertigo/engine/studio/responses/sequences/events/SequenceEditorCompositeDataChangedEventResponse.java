package com.twinsoft.convertigo.engine.studio.responses.sequences.events;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.sequences.AbstractSequenceResponse;

public class SequenceEditorCompositeDataChangedEventResponse extends AbstractSequenceResponse {

    private String internalRequesterOutput;

    public SequenceEditorCompositeDataChangedEventResponse(Sequence sequence, String internalRequesterOutput) {
        super(sequence);
        this.internalRequesterOutput = internalRequesterOutput;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "internal_requester_output", internalRequesterOutput));

        return response;
    }
}
