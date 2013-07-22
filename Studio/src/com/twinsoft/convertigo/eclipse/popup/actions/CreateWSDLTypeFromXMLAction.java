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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.TransactionWSDLTypesDialog;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.IEditableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class CreateWSDLTypeFromXMLAction extends MyAbstractAction {

	public CreateWSDLTypeFromXMLAction() {
		super();
	}

	public void run() {
		final Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	ProjectExplorerView explorerView = getProjectExplorerView();
        	if (explorerView != null) {
            	TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
            	ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();
            	
            	MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
    			String message = "Do you really want to extract the WSDL types?\nWarning: the previously stored WSDL types will be replaced.";
            	messageBox.setMessage(message);
            	if (messageBox.open() == SWT.YES) {
            		
            		RequestableObject requestable = (RequestableObject)treeObject.getObject();
                    String requestableName = StringUtils.normalize(requestable.getName(), true);
                    Document document = null;
                    String result = null;
                    
                    if (!(requestableName.equals(requestable.getName()))) {
                    	throw new Exception("Requestable name should be normalized");
                    }
                    
                    if (requestable instanceof Transaction) {
                        Connector connector = (Connector) requestable.getParent();
                        
                        String connectorName = StringUtils.normalize(connector.getName(), true);
                        if (!(connectorName.equals(connector.getName()))) {
                        	throw new Exception("Connector name should be normalized");
                        }
                        
                        if (!(requestable instanceof HtmlTransaction)) {
    	                    ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
    	                    if (connectorEditor == null) {
                        		ConvertigoPlugin.infoMessageBox("Please open connector first.");
    	                    	return;
        	                }
        	                
    	                    document = connectorEditor.getLastGeneratedDocument();
    	                    if (document == null) {
    	                    	ConvertigoPlugin.infoMessageBox("You should first generate the XML document before trying to extract the WSDL types.");
    	                    	return;
    	                    }
                        
    	                    String xsdPrefix = requestable.getXsdTypePrefix();
   	                        document.getDocumentElement().setAttribute("transaction", xsdPrefix + requestableName);
    	                    
    	                    result = requestable.generateWsdlType(document);
                        }
                        else {
                        	HtmlTransaction defaultTransaction = (HtmlTransaction)connector.getDefaultTransaction();
                        	if (defaultTransaction != null) {
                        		String defaultTransactionName = StringUtils.normalize(defaultTransaction.getName(), true);
                            	if (!(defaultTransactionName.equals(defaultTransaction.getName()))) {
                            		throw new Exception("Default transaction name should be normalized");
                            	}
                        		
        						TransactionWSDLTypesDialog dlg = new TransactionWSDLTypesDialog(shell, requestable);
        						if (dlg.open() == Window.OK) {
        							result = dlg.result;
        		        		}
        		        	}
                        	else {
                            	messageBox = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                            	messageBox.setText("Convertigo");
                            	messageBox.setMessage("Please set a default transaction on parent connector first");
                            	messageBox.open();
                        	}
                        }
                    }
                    else if (requestable instanceof Sequence) {
                    	result = requestable.generateWsdlType(document);
                    }
                    
                    if ((result != null) && (!result.equals(""))) {
						requestable.wsdlType = result;
	                    requestable.hasChanged = true;
	                    
	                    explorerView.updateDatabaseObject(requestable);
	                    if (requestable instanceof HtmlTransaction && !((HtmlTransaction)requestable).isDefault)
	                    	explorerView.updateDatabaseObject(((HtmlTransaction)requestable).getConnector().getDefaultTransaction());
	                    
	                    if (requestable instanceof Transaction)
	                    	((IEditableTreeObject) treeObject).launchEditor("XMLTransactionEditor");
	                    else if (requestable instanceof Sequence)
	                    	((IEditableTreeObject) treeObject).launchEditor("XMLSequenceEditor");
                    }
            	}
        	}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to create WSDL type!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
