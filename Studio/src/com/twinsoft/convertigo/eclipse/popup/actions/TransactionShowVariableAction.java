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
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.twinj.Javelin;

public class TransactionShowVariableAction extends MyAbstractAction {

	public TransactionShowVariableAction() {
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
						String normalizedScreenClassName = StringUtils.normalize(currentScreenClass.getName());
						
						int i;
						String handlerName = "on" + normalizedScreenClassName + JavelinTransaction.EVENT_ENTRY_HANDLER;
						if ((i = transaction.handlers.indexOf(handlerName)) == -1) {
							display.beep();
							ConvertigoPlugin.logWarning("Unable to show the position of the variable \"" + variable + "\": no handler found for the current screen class!");
						}
						else {
							ConvertigoPlugin.logDebug("Found handler: " + handlerName);
				
							// Delimit the function
							int bof, eof;
							bof = transaction.handlers.indexOf('{', i) + 2;
							eof = transaction.handlers.indexOf("function", bof);
							if (eof == -1) {
								eof = transaction.handlers.lastIndexOf('}') - 1;
							}
							else {
								eof = transaction.handlers.lastIndexOf('}', eof) - 1;
							}
							String function = transaction.handlers.substring(bof, eof);
				
							// Delimit the marker for generated input variables code
							int idxMarker = function.indexOf("\t// begin-of-variables");
							if (idxMarker == -1) {
								// No variable marker, do nothing
								display.beep();
								ConvertigoPlugin.logWarning("Unable to show the position of the variable \"" + variable + "\": no variable marker found for the handler!");
								return;
							}
				
							int idxMarker2 = function.indexOf("\t// end-of-variables", idxMarker);
				
							String code = function.substring(idxMarker, idxMarker2);
							String line = "\tjavelin.send(" + variable + ");\n";
							int idxPosition = code.indexOf(line);
							if (idxPosition == -1) {
								ConvertigoPlugin.logDebug("No variable '" + variable + "' found into the handler!");
								return;
							}
							idxPosition = code.lastIndexOf("moveCursor(", idxPosition) + 11;
							int idxComa = code.indexOf(',', idxPosition);
							int idxClosedParenthesis = code.indexOf(')', idxComa);
							int x = Integer.parseInt(code.substring(idxPosition, idxComa).trim());
							int y = Integer.parseInt(code.substring(idxComa + 1, idxClosedParenthesis).trim());
				
							ConvertigoPlugin.logDebug("Variable position found: " + x + ", " + y);
							javelin.moveCursor(x, y);
						}
								
						javelin.requestFocus();
					}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to show variable to Javelin!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
