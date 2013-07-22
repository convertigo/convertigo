/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class SetDefaultTransactionAction extends MyAbstractAction {

	public SetDefaultTransactionAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof TransactionTreeObject) {
			Transaction selectedTransaction = (Transaction)treeObject.getObject();
			action.setChecked(selectedTransaction.isDefault);
		}
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TransactionTreeObject transactionTreeObject = (TransactionTreeObject)explorerView.getFirstSelectedTreeObject();
                
    			Transaction transaction = (Transaction)explorerView.getFirstSelectedDatabaseObject();
                Connector connector = (Connector) transaction.getParent();
                
                // Report from 4.5: fix #401
                TransactionTreeObject defaultTransactionTreeObject = null;
                Transaction defaultTransaction = connector.getDefaultTransaction();
                if (defaultTransaction != null) {
	                defaultTransactionTreeObject = (TransactionTreeObject)explorerView.findTreeObjectByUserObject(defaultTransaction);
                }
    			
    			connector.setDefaultTransaction(transaction);
                if (defaultTransactionTreeObject != null) {
	                defaultTransactionTreeObject.isDefault = false;
	                defaultTransactionTreeObject.hasBeenModified(true);
                }
                transactionTreeObject.isDefault = true;
                transactionTreeObject.hasBeenModified(true);
                
                // Updating the tree
                explorerView.refreshTreeObject(transactionTreeObject.getParentDatabaseObjectTreeObject());
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to set transaction to default one!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
