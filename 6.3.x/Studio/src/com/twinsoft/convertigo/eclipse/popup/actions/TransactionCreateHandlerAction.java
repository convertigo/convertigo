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

import java.io.IOException;
import java.util.Vector;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.CreateHandlerDialog;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class TransactionCreateHandlerAction extends MyAbstractAction {

	public TransactionCreateHandlerAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof TransactionTreeObject)
				enable = !(treeObject.getObject() instanceof SiteClipperTransaction);
			else if (treeObject instanceof ObjectsFolderTreeObject)
				enable = ((ObjectsFolderTreeObject)treeObject).folderType == ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS;
			action.setEnabled(enable);
		}
		catch (Exception e) {}
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		Statement lastStatement = null;
		int index = 0;

		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				if (treeObject != null) {
					Transaction transaction = null;
					if (treeObject instanceof TransactionTreeObject)
						transaction = (Transaction)treeObject.getObject();
					else if (treeObject instanceof ObjectsFolderTreeObject)
						transaction = (Transaction)treeObject.getParent().getObject();
					
					if (transaction != null) {
						CreateHandlerDialog createHandlerDialog = new CreateHandlerDialog(shell, transaction);
						createHandlerDialog.open();
			    		if (createHandlerDialog.getReturnCode() != Window.CANCEL) {
			    			Vector<?> result = createHandlerDialog.result;
			    			if (result != null) {
			    				int len = result.size();
			    				if (len > 0) {
				    				if (transaction instanceof HtmlTransaction) {
				    					HtmlTransaction htmlTransaction = (HtmlTransaction)transaction;
				    					Statement statement = null;
				    					for (int i=0; i<len; i++) {
				    						statement = (Statement)result.elementAt(i);
			    							htmlTransaction.addStatement(statement);
				    					}
				    					lastStatement = statement;
				    				}
				    				else {
				    					String handler = null;
				    					for (int i=0; i<len; i++) {
				    						handler = (String)result.elementAt(i);
					    					transaction.handlers += handler;
					    					transaction.hasChanged = true;
					    					
											// Update the opened handlers editor if any
											IEditorPart jspart = ConvertigoPlugin.getDefault().getJscriptTransactionEditor(transaction);
											if ((jspart != null) && (jspart instanceof JscriptTransactionEditor)) {
												JscriptTransactionEditor jscriptTransactionEditor = (JscriptTransactionEditor)jspart;
												jscriptTransactionEditor.reload();
											}
											index = i;
				    					}
				    				}
				    				
				    				// Reload transaction in tree and select last created Statement.
	            					try {
	            						ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
	            						projectExplorerView.reloadDatabaseObject(transaction);
	            						
	            						
	            						if (transaction instanceof HtmlTransaction) {
		            						if (lastStatement != null) {
		            							projectExplorerView.objectSelected(new CompositeEvent(lastStatement));
		            						}
	            						}
		            					else {
		            						TreeViewer treeViewer = explorerView.viewer;
		            						Tree tree = treeViewer.getTree();				
		            						TreeItem[] treeItems = tree.getSelection();
		            						treeItems[0].setExpanded(true);
		            						treeViewer.refresh();
		            					    TreeItem[] transactionItems = treeItems[0].getItems();
		            					    transactionItems[0].setExpanded(true);
		            					    treeViewer.refresh();
		            					    TreeItem[] handlersItems = transactionItems[0].getItems();
		            					    tree.deselect(treeItems[0]);
		            					    boolean found = false;
		            					    int i=0;
		            					    while (!found) {
		            					    	if (((String)result.elementAt(index)).contains(handlersItems[i].getText())) {
		            					    		tree.select(handlersItems[i]);
		            					    		found = true;
		            					    	}
		            					    	i++;
		            					    }		            					 
		            					    treeViewer.refresh();     					    

		            					    IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
		            						TreeObject treeObjects = (TreeObject) selection.getFirstElement();
		            						
		            						StructuredSelection structuredSelection = new StructuredSelection(treeObjects);
		            						ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)projectExplorerView, structuredSelection);
		            					}
	            						
									} catch (IOException e) {}
	    	        				
			    				}
			    			}
			    		}
					}
				}
    		}
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to create new handler for transaction!");
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
	
}
