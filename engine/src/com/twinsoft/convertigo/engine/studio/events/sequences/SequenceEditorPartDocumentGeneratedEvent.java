package com.twinsoft.convertigo.engine.studio.events.sequences;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorPartDocumentGeneratedEventResponse;

public class SequenceEditorPartDocumentGeneratedEvent extends AbstractSequenceEvent {

    private String sequenceOutput;

    public SequenceEditorPartDocumentGeneratedEvent(Sequence sequence, String sequenceOutput) {
        super("SequenceEditorPart.documentGenerated", sequence);
        this.sequenceOutput = sequenceOutput;
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorPartDocumentGeneratedEventResponse(sequence, sequenceOutput).toXml(document, qname);
    }
}
