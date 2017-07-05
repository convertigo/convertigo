package com.twinsoft.convertigo.engine.studio.events.sequence;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.events.AbstractSequenceEvent;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorDocumentGeneratedEventResponse;

public class SequenceEditorDocumentGeneratedEvent extends AbstractSequenceEvent {

    private String sequenceOutput;

    public SequenceEditorDocumentGeneratedEvent(Sequence sequence, String sequenceOutput) {
        super("SequenceEditor.documentGenerated", sequence);
        this.sequenceOutput = sequenceOutput;
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorDocumentGeneratedEventResponse(sequence, sequenceOutput).toXml(document, qname);
    }
}
