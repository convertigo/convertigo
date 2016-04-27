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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.requesters.DefaultRequester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CreateFormFromTransactionAction extends MyAbstractAction {

	public CreateFormFromTransactionAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			Object ob = treeObject.getObject();
			enable = (ob instanceof TransactionWithVariables) && !(ob instanceof SiteClipperTransaction);
			action.setEnabled(enable);
		}
		catch (Exception e) {}
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	ProjectExplorerView explorerView = getProjectExplorerView();
        	if (explorerView != null) {
        		Transaction transaction = null;
        		Connector connector = null;
        		
            	TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
            	
        		if (treeObject instanceof TransactionTreeObject) {
        			transaction = (Transaction)treeObject.getObject();
        			connector = (Connector) transaction.getParent();
        			if (transaction instanceof TransactionWithVariables) {
        				TransactionWithVariables tr = (TransactionWithVariables)transaction;
        				//if (tr.getVariablesDefinitionSize() == 0) {
        				if (tr.numberOfVariables() == 0) {
		    	        	MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.APPLICATION_MODAL);
		    	        	messageBox.setMessage("No variables are defined for transaction " + tr.getName());
		    	        	messageBox.open();
        				} else { 
        					String xslContent = generateXSLForm(connector, tr);
        					
        					// Refresh project resource
        					IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(tr.getProject().getName());
        					
        			    	// Compute the targetXSL file name
        			    	String 	xslForm = tr.getName()+"Request.xsl";
        			    	
        					IFile file = project.getFile(xslForm);
        					ByteArrayInputStream bais = new ByteArrayInputStream(xslContent.getBytes());
        					if (file.exists()) {
	        					file.setContents((InputStream)bais, IFile.FORCE | IFile.KEEP_HISTORY, null);
        					} else {
        						file.create((InputStream)bais, true, null);
        					}
        				
        					TransactionWithVariables newTrans = (TransactionWithVariables)tr.clone();
        					newTrans.hasChanged = true;
        					newTrans.bNew = true;
        					XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();
        					orderedVariables.add(new XMLVector<Long>());
        					newTrans.setOrderedVariables(orderedVariables);
        					newTrans.setSheetLocation(Transaction.SHEET_LOCATION_FROM_REQUESTABLE);
        					
        					HandlerStatement handler = new HandlerStatement(HandlerStatement.EVENT_TRANSACTION_STARTED, HandlerStatement.RETURN_CANCEL);
        					newTrans.add(handler);
        					
        					newTrans.setName(tr.getName()+"Request");
        					Sheet sheet = new Sheet();
        					sheet.setBrowser("*");
        					sheet.setUrl(xslForm);
        					newTrans.addSheet(sheet);
        					
    						connector.add(newTrans);
        					postCreate(treeObject.getConnectorTreeObject(), newTrans);
        				}
        			}
        		}
        	}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to generate a requestForm widget");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
    private String  generateXSLForm(Connector connector, TransactionWithVariables tr) throws Exception {
    
    	// Build a source DOM containing all the variable part of the generatedXSL file
    	Document doc = new DefaultRequester().createDomWithNoXMLDeclaration(tr.getEncodingCharSet());
    	Element variables = doc.createElement("variables");
    	doc.appendChild(variables);
    	
    	
		/*int size = tr.getVariablesDefinitionSize();
		for (int i = 0 ; i < size ; i++) {
			Element variable = doc.createElement("variable");
			
			Element elt = doc.createElement("variableDefinitionName");
			elt.appendChild(doc.createTextNode(tr.getVariableDefinitionName(i)));
			variable.appendChild(elt);

			elt = doc.createElement("variableDefinitionDefaultValue");
			elt.appendChild(doc.createTextNode((String)tr.getVariableDefinitionDefaultValue(i)));
			variable.appendChild(elt);
			
			elt = doc.createElement("VariableDefinitionDescription");
			elt.appendChild(doc.createTextNode(tr.getVariableDefinitionDescription(i)));
			variable.appendChild(elt);
			
			variables.appendChild(variable);
		}*/
		int size = tr.numberOfVariables();
		for (int i = 0 ; i < size ; i++) {
			RequestableVariable rVariable = (RequestableVariable)tr.getVariable(i);
			if (rVariable != null) {
				Element variable = doc.createElement("variable");
				
				Element elt = doc.createElement("variableDefinitionName");
				elt.appendChild(doc.createTextNode(rVariable.getName()));
				variable.appendChild(elt);

				elt = doc.createElement("variableDefinitionDefaultValue");
				elt.appendChild(doc.createTextNode(rVariable.getDefaultValue().toString()));
				variable.appendChild(elt);
				
				elt = doc.createElement("VariableDefinitionDescription");
				elt.appendChild(doc.createTextNode(rVariable.getDescription()));
				variable.appendChild(elt);
				
				variables.appendChild(variable);
			}
		}
    	
    	
		Element elt = doc.createElement("transactionName");
		elt.appendChild(doc.createTextNode(tr.getName()));
		variables.appendChild(elt);
    	
    	// Transform using the GenerateForm.xsl template
        //System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        //TransformerFactory tFactory = TransformerFactory.newInstance();
		TransformerFactory tFactory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
        String filePath = Engine.TEMPLATES_PATH + "/xsl/GenerateForm.xsl";
        StreamSource streamSource = new StreamSource(new File(filePath).toURI().toASCIIString());
        Transformer transformer = tFactory.newTransformer(streamSource);
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
    	String  xslContent = sw.toString();
    	xslContent = XMLUtils.prettyPrintDOM(xslContent);
        return xslContent;
    }

    
	public void postCreate(TreeObject parentTreeObject, DatabaseObject createdDatabaseObject) throws Exception {
		ProjectExplorerView explorerView = getProjectExplorerView();
		explorerView.reloadTreeObject(parentTreeObject);
		explorerView.objectSelected(new CompositeEvent(createdDatabaseObject));
		
		/* No more needed since #20 correction : see DatabaseObjectTreeObject:setParent(TreeParent parent)
		TreeObject selectedTreeObject = explorerView.getFirstSelectedTreeObject();
		if ((selectedTreeObject != null) && (selectedTreeObject.getObject().equals(createdDatabaseObject))) {
			explorerView.fireTreeObjectAdded(new TreeObjectEvent(selectedTreeObject));
		}*/
	}
    
}
