package com.twinsoft.convertigo.engine.studio.editors.sequences;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.editors.EditorPartWrap;

public class SequenceEditorWrap extends EditorPartWrap {
    private SequenceEditorPartWrap sequenceEditorPart;
    private boolean dirty;

    public SequenceEditorWrap(Sequence sequence) {
        sequenceEditorPart = new SequenceEditorPartWrap(this, sequence);
    }

    public SequenceEditorPartWrap getSequenceEditorPart() {
        return sequenceEditorPart;
    }

    public void getDocument(String sequenceName, boolean isStubRequested) {
        getDocument(sequenceName, null, isStubRequested);
    }

    public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
        sequenceEditorPart.getDocument(sequenceName, testcaseName, isStubRequested);
    }

    public Document getLastGeneratedDocument() {
        return sequenceEditorPart.lastGeneratedDocument;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
