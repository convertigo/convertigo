package com.twinsoft.convertigo.engine.studio.events;

import com.twinsoft.convertigo.beans.core.Sequence;

public abstract class AbstractSequenceEvent extends GenericEvent {

    protected Sequence sequence;

    public AbstractSequenceEvent(String name, Sequence sequence) {
        super(name, sequence.getQName());
        this.sequence = sequence;
    }
}
