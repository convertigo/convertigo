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

package com.twinsoft.convertigo.engine.studio.popup.actions;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.Studio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.ConnectorEditorWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ConnectorView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class TransactionExecuteDefaultAction extends AbstractRunnableAction {

    public TransactionExecuteDefaultAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            if (treeObject != null) {
                ProjectView projectTreeObject = treeObject.getProjectViewObject();
    
                Connector connector = null;
                if (treeObject.instanceOf(Connector.class)) {
                    ConnectorView connectorTreeObject = (ConnectorView) treeObject;
                    connectorTreeObject.openConnectorEditor();
                    connector = connectorTreeObject.getObject();
                }
                else {
                    connector = ((Project) projectTreeObject.getObject()).getDefaultConnector();
                    ConnectorView connectorTreeObject = (ConnectorView) Studio.getViewFromDbo(connector, studio);
                    if (connectorTreeObject != null) {
                        connectorTreeObject.openConnectorEditor();
                    }
                }
    
                Transaction transaction = connector.getDefaultTransaction();
                ConnectorEditorWrap connectorEditor = projectTreeObject.getConnectorEditor(connector);
                if (connectorEditor != null) {
                    //getActivePage().activate(connectorEditor);
                    connectorEditor.getDocument(transaction.getName(), false);
                }
            }
        }
        catch (Exception e) {
            throw e;
        }
    }
}
