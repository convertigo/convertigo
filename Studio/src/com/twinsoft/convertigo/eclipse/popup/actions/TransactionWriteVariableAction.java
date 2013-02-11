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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorPart;
import com.twinsoft.convertigo.eclipse.editors.connector.JavelinConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.twinj.Javelin;

public class TransactionWriteVariableAction extends MyAbstractAction {

	public TransactionWriteVariableAction() {
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
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				String variable = (String) ((VariableTreeObject) treeObject).getObject();
				JavelinTransaction transaction = (JavelinTransaction)treeObject.getParent().getParent().getObject();
				JavelinConnector javelinConnector = (JavelinConnector)transaction.getParent();
				
				IEditorPart wpart = getConnectorEditor(javelinConnector);
				if ((wpart != null) && (wpart instanceof ConnectorEditor)) {
					getActivePage().activate(wpart);
					ConnectorEditor connectorEditor = (ConnectorEditor)wpart;
					ConnectorEditorPart connectorEditorPart = connectorEditor.getConnectorEditorPart();
					AbstractConnectorComposite connectorComposite = connectorEditorPart.getConnectorComposite();
					if ((connectorComposite != null) && (connectorComposite instanceof JavelinConnectorComposite)) {
						Javelin javelin = ((JavelinConnectorComposite)connectorComposite).getJavelin();
						ScreenClass currentScreenClass = ((JavelinConnector) connectorEditorPart.getConnector()).getCurrentScreenClass();
						ConvertigoPlugin.logDebug("Analyzing screen class '" + currentScreenClass.getName() + "'...");
						String normalizedScreenClassName = StringUtils.normalize(currentScreenClass.getName());
						
						int i;
						String handlerName = "on" + normalizedScreenClassName + JavelinTransaction.EVENT_ENTRY_HANDLER;
						ConvertigoPlugin.logDebug("Handlers:\n" + transaction.handlers);
						ConvertigoPlugin.logDebug("Searching for handler '" + handlerName + "'...");
						if ((i = transaction.handlers.indexOf(handlerName)) == -1) {
							display.beep();
							ConvertigoPlugin.logDebug("No handler found for the current screen class!");
						}
						else {
							ConvertigoPlugin.logDebug("Handler found!");

							// Delimit the function
							int bof, eof;
							bof = transaction.handlers.indexOf('{', i) + 1;
							eof = transaction.handlers.indexOf("function", bof);
							if (eof == -1) {
								eof = transaction.handlers.lastIndexOf('}') - 1;
							}
							else {
								eof = transaction.handlers.lastIndexOf('}', eof) - 1;
							}
							String function = transaction.handlers.substring(bof, eof);

							int c = javelin.getCurrentColumn();
							int l = javelin.getCurrentLine();
							
							String line1 = "\tjavelin.moveCursor(" + c + ", " + l + ");\n";
							
							// We must remove the default value of the variable if any
							String variableName = variable.toString();
							int ii;
							if ((ii = variableName.indexOf(' ')) != -1) {
								variableName = variableName.substring(0, ii);
							}
							String line2 = "\tjavelin.send(" + variableName + ");\n"; 

							// Delimit the marker for generated input variables code
							String code = "";
							int idxMarker = function.indexOf("\t// begin-of-variables");
							if (idxMarker == -1) {
								code = "\n\t// begin-of-variables: DO NOT EDIT OR MODIFY\n";
								code += line1 + line2;
								code += "\t// end-of-variables\n";

								function = code + function; 
							}
							else {
								idxMarker = function.indexOf("\t// end-of-variables");

								// Update previous definition if any
								int idxPreviousDefinition = function.indexOf(line2);
								if (idxPreviousDefinition != -1) {
									int i1 = function.lastIndexOf("moveCursor(", idxPreviousDefinition) + 11;
									
									// Search for moveCursor only inside the variables block
									if (i1 < idxMarker) {
										int i2 = function.indexOf(')', i1);
										function = function.substring(0, i1) + c + ", " + l + function.substring(i2);
									}
								}
								// Add definition otherwise
								else {
									code += line1 + line2;
								}
								function = function.substring(0, idxMarker) + code + function.substring(idxMarker); 
							}

							transaction.handlers = transaction.handlers.substring(0, bof) + function + transaction.handlers.substring(eof);
							transaction.hasChanged = true;
							
							ConvertigoPlugin.logDebug("Code added:\n" + code);
							
							explorerView.updateDatabaseObject(transaction);
							
							// Updating the opened handlers editor if any
							IEditorPart jspart = getJscriptTransactionEditor(transaction);
							if ((jspart != null) && (jspart instanceof JscriptTransactionEditor)) {
								JscriptTransactionEditor jscriptTransactionEditor = (JscriptTransactionEditor)jspart;
								jscriptTransactionEditor.reload();
							}
						}
								
						javelin.requestFocus();
					}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to write variable from Javelin!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
