package com.twinsoft.convertigo.engine.studio.wrappers;

import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.editors.sequence.SequenceEditorWrap;

public class ProjectView extends DatabaseObjectView {

    private static Map<Sequence, SequenceEditorWrap> sequenceToSequenceEditor = new HashMap<>();

    public ProjectView(Project project, WrapStudio studio) {
        super(project, studio);
    }

    public void closeAllEditors() {  
    }

    public void closeSequenceEditors(Sequence sequence) {
    }

    public void closeConnectorEditors(Connector connector) {
    }

    public SequenceEditorWrap getSequenceEditor(Sequence sequence) {
        SequenceEditorWrap sequenceEditor = sequenceToSequenceEditor.get(sequence);
        // Create instance if it doesn't exist yet
        if (sequenceEditor == null) {
            sequenceEditor = new SequenceEditorWrap(sequence);
            sequenceToSequenceEditor.put(sequence, sequenceEditor);
        }

        return sequenceEditor;
    }
}
