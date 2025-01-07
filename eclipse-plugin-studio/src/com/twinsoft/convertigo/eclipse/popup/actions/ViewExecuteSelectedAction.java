/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetViewTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class ViewExecuteSelectedAction extends MyAbstractAction {

	public ViewExecuteSelectedAction() {
		super();
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				if ((treeObject != null) && (treeObject instanceof DesignDocumentViewTreeObject)) {
					DesignDocumentViewTreeObject viewTreeObject = (DesignDocumentViewTreeObject)treeObject;
					DesignDocumentTreeObject ddto = (DesignDocumentTreeObject) viewTreeObject.getTreeObjectOwner();
					ConnectorTreeObject cto = (ConnectorTreeObject) ddto.getOwnerDatabaseObjectTreeObject();
					ProjectTreeObject projectTreeObject = cto.getProjectTreeObject();
					CouchDbConnector connector = (CouchDbConnector) cto.getObject();
					cto.openConnectorEditor();

					ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
					if (connectorEditor != null) {
						// activate connector's editor
						getActivePage().activate(connectorEditor);

						// set transaction's parameters
						Transaction transaction = connector.getTransactionByName(CouchDbConnector.internalView);
						((GetViewTransaction)transaction).setViewname(viewTreeObject.getDocViewName());

						Variable view_reduce = ((GetViewTransaction)transaction).getVariable(CouchParam.prefix + "reduce");
						view_reduce.setValueOrNull(viewTreeObject.hasReduce() ? isReduceRequested():false);

						// execute view transaction
						connectorEditor.getDocument(CouchDbConnector.internalView, isStubRequested());
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to execute the selected view!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof DesignDocumentViewTreeObject) {
			action.setEnabled(((DesignDocumentViewTreeObject)treeObject).hasReduce());
		}
	}

	private boolean isStubRequested() {
		return false;
	}

	protected boolean isReduceRequested() {
		return true;
	}
}
