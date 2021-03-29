/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.ConnectorEditorWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.TransactionView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class TransactionExecuteSelectedAction extends AbstractRunnableAction {

	public TransactionExecuteSelectedAction(WrapStudio studio) {
		super(studio);
	}

	@Override
	public void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();

			if ((treeObject != null) && (treeObject.instanceOf(Transaction.class))) {
			    TransactionView transactionTreeObject = (TransactionView) treeObject;

				Transaction transaction = transactionTreeObject.getObject();
				transactionTreeObject.getConnectorTreeObject().openConnectorEditor();

				Connector connector = (Connector) transaction.getParent();

				ProjectView projectTreeObject = transactionTreeObject.getProjectView();
				ConnectorEditorWrap connectorEditor = projectTreeObject.getConnectorEditor(connector);
				if (connectorEditor != null) {
					//getActivePage().activate(connectorEditor);
					connectorEditor.getDocument(transaction.getName(), isStubRequested());
				}
			}
        }
        catch (Throwable e) {
        	//ConvertigoPlugin.logException(e, "Unable to execute the selected transaction!");
        	throw e;
        }
	}

	protected boolean isStubRequested() {
		return false;
	}
}
