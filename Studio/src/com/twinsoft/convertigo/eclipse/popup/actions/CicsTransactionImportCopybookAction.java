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

import java.io.FileReader;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.transactions.CicsTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;


public class CicsTransactionImportCopybookAction extends MyAbstractAction {

	public CicsTransactionImportCopybookAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	boolean bInputMap = true;  	
        	CustomDialog customDialog = new CustomDialog(
					shell,
					"Import a copybook",
					"Do you want to import the copybook into the transaction input map or output map?",
					500, 150,
					new ButtonSpec("Input map", true),
					new ButtonSpec("Outut map", false),
					new ButtonSpec(IDialogConstants.CANCEL_LABEL, false)
			);
			int index = customDialog.open();
        	switch (index) {
        		case 0:		bInputMap = true; break;
        		case 1:		bInputMap = false; break;
        		case 2: 	return;
        	}
        	String filePath = null;
        	FileDialog fileDialog = new FileDialog(shell);
        	fileDialog.setText("Import a copybook");
        	fileDialog.setFilterPath(Engine.PROJECTS_PATH);
        	filePath = fileDialog.open();
        	if (filePath != null) {
        		ConvertigoPlugin.logDebug("Import copybook from file \"" + filePath + "\"");
        		ProjectExplorerView explorerView = getProjectExplorerView();
        		if (explorerView != null) {
        			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
        			Object databaseObject = treeObject.getObject();
        			if ((databaseObject != null) && (databaseObject instanceof CicsTransaction)) {
           				CicsTransaction transaction = (CicsTransaction)databaseObject;
            			FileReader reader = null;
        				
            			try {
        					reader = new FileReader(filePath);
        					transaction.importCopyBook(bInputMap, reader);
        				}
        				finally {
        					if (reader != null) {
        						reader.close();
        					}
        				}

        				explorerView.updateFirstSelectedTreeObject();
						StructuredSelection structuredSelection = new StructuredSelection(treeObject);
						ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged(explorerView, structuredSelection);
        				
        			}
        		}
        	}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to import the copybook!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
