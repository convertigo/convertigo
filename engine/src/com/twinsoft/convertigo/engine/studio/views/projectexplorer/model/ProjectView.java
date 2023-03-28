/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

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
