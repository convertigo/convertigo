package com.twinsoft.convertigo.engine.studio.responses.sequences.events;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.sequences.AbstractSequenceResponse;

public class SequenceEditorPartDocumentGeneratedEventResponse extends AbstractSequenceResponse {

    private String sequenceOutput;

    public SequenceEditorPartDocumentGeneratedEventResponse(Sequence sequence, String sequenceOutput) {
        super(sequence);
        this.sequenceOutput = sequenceOutput;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "sequence_output", sequenceOutput));

        return response;
    }
}
