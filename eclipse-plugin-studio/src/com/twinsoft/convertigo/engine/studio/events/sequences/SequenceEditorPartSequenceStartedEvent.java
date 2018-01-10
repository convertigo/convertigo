package com.twinsoft.convertigo.engine.studio.events.sequences;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorPartSequenceStartedEventResponse;

public class SequenceEditorPartSequenceStartedEvent extends AbstractSequenceEvent {

    public SequenceEditorPartSequenceStartedEvent(Sequence sequence) {
        super("SequenceEditorPart.sequenceStarted", sequence);
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorPartSequenceStartedEventResponse(sequence).toXml(document, qname);
    }
}
