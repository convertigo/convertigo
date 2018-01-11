package com.twinsoft.convertigo.engine.studio.editors.sequences;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.SequenceEvent;
import com.twinsoft.convertigo.beans.core.Step;

public abstract class AbstractSequenceCompositeWrap {

    protected SequenceEditorPartWrap sequenceEditorPart;
    protected Sequence sequence;

    public AbstractSequenceCompositeWrap(SequenceEditorPartWrap sequenceEditorPart, Sequence sequence) {
        this.sequenceEditorPart = sequenceEditorPart;
        this.sequence = sequence;
    }

    protected boolean checkEventSource(EventObject event) {
        boolean isSourceFromSequence = false;
        Object source = event.getSource();
        if (event instanceof SequenceEvent) {
            if ((source instanceof Sequence) || (source instanceof Step)) {
                Sequence sequence = null;
                if (source instanceof Sequence) sequence = (Sequence)source;
                if (source instanceof Step) sequence = ((Step)source).getParentSequence();
                if ((sequence != null) && (sequence.equals(this.sequence) || sequence.getOriginal().equals(this.sequence)))
                    isSourceFromSequence = true;
            }
        }
        return isSourceFromSequence;
    }

    protected abstract void clearContent();
}
