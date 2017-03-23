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

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class EnableStatementAction extends MyAbstractAction {

	public EnableStatementAction() {
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
    			DatabaseObjectTreeObject treeObject = null;
    			Statement statement = null;
    			
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
				for (int i = treeObjects.length-1 ; i>=0  ; i--) {
					treeObject = (DatabaseObjectTreeObject) treeObjects[i];
					if (treeObject instanceof StatementTreeObject) {
						StatementTreeObject statementTreeObject = (StatementTreeObject)treeObject;
						statement = (Statement)statementTreeObject.getObject();
						statement.setEnabled(true);
						
						statementTreeObject.setEnabled(true);
						statementTreeObject.hasBeenModified(true);
					}
				}
				
				explorerView.refreshSelectedTreeObjects();
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to enable statement!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
}
