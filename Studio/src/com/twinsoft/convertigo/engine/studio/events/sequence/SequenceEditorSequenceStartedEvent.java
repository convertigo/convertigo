package com.twinsoft.convertigo.engine.studio.events.sequence;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.events.AbstractSequenceEvent;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorSequenceStartedEventResponse;

public class SequenceEditorSequenceStartedEvent extends AbstractSequenceEvent {

    public SequenceEditorSequenceStartedEvent(Sequence sequence) {
        super("SequenceEditor.sequenceStarted", sequence);
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorSequenceStartedEventResponse(sequence).toXml(document, qname);
    }
}
