package com.twinsoft.convertigo.engine.studio.events.sequences;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorPartSequenceFinishedEventResponse;

public class SequenceEditorPartSequenceFinishedEvent extends AbstractSequenceEvent {

    public SequenceEditorPartSequenceFinishedEvent(Sequence sequence) {
        super("SequenceEditorPart.sequenceFinished", sequence);
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorPartSequenceFinishedEventResponse(sequence).toXml(document, qname);
    }
}
