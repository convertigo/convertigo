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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_object.NewObjectWizard;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CreateSheetFromXMLAction extends MyAbstractAction {

	public CreateSheetFromXMLAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	ProjectExplorerView explorerView = getProjectExplorerView();
        	if (explorerView != null) {
        		ScreenClass lastDetectedScreenClass = null;
        		Transaction transaction = null;
        		DatabaseObject parent = null;
        		Connector connector = null;
        		
            	TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
            	ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();
            	
        		if (treeObject instanceof TransactionTreeObject) {
        			transaction = (Transaction)treeObject.getObject();
        			connector = (Connector) transaction.getParent();
        			parent = transaction;
        		}
        		
        		if (connector != null) {
        			ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
        			lastDetectedScreenClass = connectorEditor.getLastDetectedScreenClass();
        			Document lastGeneratedDocument = connectorEditor.getLastGeneratedDocument();
        			
        			if (lastGeneratedDocument == null) {
        				ConvertigoPlugin.logWarning("You must first generate an XML document before create a style sheet from the last generated XML document.");
        				return;
        			}
        			
        			if (lastDetectedScreenClass != null) {
    					CustomDialog customDialog = new CustomDialog(
    							shell,
    							"Create a new sheet",
    							"Would you like to create a new sheet for the last detected screen class or a transaction?",
    							500, 150,
    							new ButtonSpec("Last detected screen class", true),
    							new ButtonSpec("Transaction", false),
    							new ButtonSpec(IDialogConstants.CANCEL_LABEL, false)
    					);
    					int ret = customDialog.open();
	                	if (ret == 0) {
	                		parent = lastDetectedScreenClass;
	                	}
	                	else if (ret == 2) {
	                		return;
	                	}
        			}
        			
        			if (parent != null) {
        	            NewObjectWizard newObjectWizard = new NewObjectWizard(parent, "com.twinsoft.convertigo.beans.core.Sheet");
                		WizardDialog wzdlg = new WizardDialog(shell, newObjectWizard);
                		wzdlg.setPageSize(850, 650);
                		wzdlg.open();
                		if (wzdlg.getReturnCode() != Window.CANCEL) {
            	            Sheet sheet = (Sheet)newObjectWizard.newBean;
            	            sheet.setName("HTML_40");
            	            sheet.setBrowser("*");
            	            sheet.setUrl(StringUtils.normalize(parent.getName()) + "-html40.xsl");
                			
                            String xsl = generateXSL(lastGeneratedDocument, "html40");
                            BufferedWriter bw = new BufferedWriter(new FileWriter(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProjectName + "/" + sheet.getUrl()));
                            bw.write(xsl);
                            bw.close();
                			
                			explorerView.reloadTreeObject(treeObject);
                		}
        			}
        		}
        	}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to add a new sheet automatically from the generated XML document!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
    private String generateXSL(Document document, String mediaType) throws Exception {
        //System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        //TransformerFactory tFactory = TransformerFactory.newInstance();
    	TransformerFactory tFactory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
        String filePath = Engine.TEMPLATES_PATH + "/xsl/" + mediaType + ".xsl";
        StreamSource streamSource = new StreamSource(new File(filePath).toURI().toASCIIString());
        Transformer transformer = tFactory.newTransformer(streamSource);
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));
        String xsl = sw.toString();

        try {
            xsl = XMLUtils.prettyPrintDOM(xsl, "file://" + Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProjectName + "/");
        }
        catch(Exception e) {
        	ConvertigoPlugin.logException(e, "Unable to pretty print the generated XSL file!");
        }
		return xsl;
    }
	
}
