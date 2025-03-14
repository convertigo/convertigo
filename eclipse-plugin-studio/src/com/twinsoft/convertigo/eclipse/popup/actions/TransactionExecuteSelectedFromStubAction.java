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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class TransactionExecuteSelectedFromStubAction extends MyAbstractAction {

	private String stubFileName = null;
	
	public TransactionExecuteSelectedFromStubAction() {
		super();
	}

	public TransactionExecuteSelectedFromStubAction(String stubFileName) {
		super();
		this.stubFileName = stubFileName;
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			if ((treeObject != null) && (treeObject instanceof TransactionTreeObject)) {
    				TransactionTreeObject transactionTreeObject = (TransactionTreeObject)treeObject;
    				
    				Transaction transaction = transactionTreeObject.getObject();
    				transactionTreeObject.getConnectorTreeObject().openConnectorEditor();
    				
    				Connector connector = (Connector)transaction.getParent();
    				ProjectTreeObject projectTreeObject = transactionTreeObject.getProjectTreeObject();
    				ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
    				if (connectorEditor != null) {
    					getActivePage().activate(connectorEditor);
    					connectorEditor.getDocument(transaction.getName(), null, stubFileName, true);
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to execute the selected transaction!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
