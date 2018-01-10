package com.twinsoft.convertigo.engine.studio.events.sequences;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.events.SequenceEditorCompositeDataChangedEventResponse;

public class SequenceEditorCompositeDataChangedEvent extends AbstractSequenceEvent {

    private String internalRequesterOutput;

    public SequenceEditorCompositeDataChangedEvent(Sequence sequence, String internalRequesterOutput) {
        super("SequenceEditorComposite.dataChanged", sequence);
        this.internalRequesterOutput = internalRequesterOutput;
    }

    @Override
    protected Element toXml() throws Exception {
        return new SequenceEditorCompositeDataChangedEventResponse(sequence, internalRequesterOutput).toXml(document, qname);
    }
}
