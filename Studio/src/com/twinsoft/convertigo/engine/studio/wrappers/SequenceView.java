package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.editors.IEditorPartWrap;
import com.twinsoft.convertigo.engine.studio.responses.sequences.SequenceExecuteSelectedOpenSequenceEditor;

public class SequenceView extends DatabaseObjectView {

    public SequenceView(Sequence sequence, WrapStudio studio) {
        super(sequence, studio);
    }

    public ProjectView getProjectView() {
        return (ProjectView) getParent();
    }

    @Override
    public Sequence getObject() {
        return (Sequence) dbo;
    }

    public void openSequenceEditor() {
        // To add listeners
        getSequenceEditor();

        synchronized (studio) {
            try {
                studio.createResponse(
                    new SequenceExecuteSelectedOpenSequenceEditor(getObject())
                        .toXml(studio.getDocument(), getObject().getQName())
                );
            }
            catch (Exception e1) {
            }

            studio.notify();

            try {
                studio.wait();
            }
            catch (InterruptedException e) {
            }
        }
    }

    private IEditorPartWrap getSequenceEditor() {
        return getProjectView().getSequenceEditor(getObject());
    }
}
