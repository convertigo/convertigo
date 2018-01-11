package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.ConnectorEditorWrap;
import com.twinsoft.convertigo.engine.studio.editors.sequences.SequenceEditorWrap;

public class ProjectView extends DatabaseObjectView {

    private static Map<Sequence, SequenceEditorWrap> sequenceToSequenceEditor = new HashMap<>();
    private static Map<Connector, ConnectorEditorWrap> connectorToConnectorEditor = new HashMap<>();

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

    public ConnectorEditorWrap getConnectorEditor(Connector connector) {
        ConnectorEditorWrap connectorEditor = connectorToConnectorEditor.get(connector);

        // Create instance if it doesn't exist yet
        if (connectorEditor == null) {
            connectorEditor = new ConnectorEditorWrap(connector);
            connectorToConnectorEditor.put(connector, connectorEditor);
        }

        return connectorEditor;
    }

    @Override
    public Project getObject() {
        return (Project) dbo;
    }
}
