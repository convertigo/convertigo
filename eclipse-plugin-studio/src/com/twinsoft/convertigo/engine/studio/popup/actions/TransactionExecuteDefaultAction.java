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
