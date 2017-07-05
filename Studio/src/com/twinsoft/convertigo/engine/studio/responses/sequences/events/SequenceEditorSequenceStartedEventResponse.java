package com.twinsoft.convertigo.engine.studio.responses.sequences.events;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.responses.sequences.AbstractSequenceEditorResponse;

public class SequenceEditorSequenceStartedEventResponse extends AbstractSequenceEditorResponse {

    public SequenceEditorSequenceStartedEventResponse(Sequence sequence) {
        super(sequence);
    }
}
