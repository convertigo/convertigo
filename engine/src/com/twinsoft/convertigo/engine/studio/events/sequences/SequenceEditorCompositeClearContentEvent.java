package com.twinsoft.convertigo.engine.studio.events.sequences;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorCompositeClearContentEventResponse;

public class SequenceEditorCompositeClearContentEvent extends AbstractSequenceEvent {

    public SequenceEditorCompositeClearContentEvent(Sequence sequence) {
        super("SequenceEditorComposite.clearContent", sequence);
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorCompositeClearContentEventResponse(sequence).toXml(document, qname);
    }
}
