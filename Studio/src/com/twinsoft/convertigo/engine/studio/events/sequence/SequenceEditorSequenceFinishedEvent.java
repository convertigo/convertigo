package com.twinsoft.convertigo.engine.studio.events.sequence;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.events.AbstractSequenceEvent;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorSequenceFinishedEventResponse;

public class SequenceEditorSequenceFinishedEvent extends AbstractSequenceEvent {

    public SequenceEditorSequenceFinishedEvent(Sequence sequence) {
        super("SequenceEditor.sequenceFinished", sequence);
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorSequenceFinishedEventResponse(sequence).toXml(document, qname);
    }
}
