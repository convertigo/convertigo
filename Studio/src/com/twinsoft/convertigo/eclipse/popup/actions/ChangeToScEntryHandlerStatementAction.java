/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/7.1.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToScEntryHandlerStatementAction.java $
 * $Author: julienda $
 * $Revision: 34593 $
 * $Date: 2013-07-25 17:22:06 +0200 (ven., 25 juil. 2014) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.statements.ScEntryHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitHandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ChangeToScEntryHandlerStatementAction extends MyAbstractAction {

	public ChangeToScEntryHandlerStatementAction() { }

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
    			// For ScExitHandler statement
    			if ((databaseObject != null) && (databaseObject instanceof ScExitHandlerStatement)) {
    				ScExitHandlerStatement scExitHandlerStatement = (ScExitHandlerStatement) databaseObject;
    				
    				if (scExitHandlerStatement.hasStatements()) {
    					
    					List<Statement> list = scExitHandlerStatement.getStatements();
    					TreePath[] selectedPaths = new TreePath[list.size()];
    					for (int i=0; i<list.size(); i++) {
    						StatementTreeObject statementTreeObject = (StatementTreeObject)explorerView.findTreeObjectByUserObject(list.get(i));
    						selectedPaths[i] = new TreePath(statementTreeObject);
    					}
    						
						TreeParent treeParent = treeObject.getParent();
						DatabaseObjectTreeObject parentTreeObject = null;
						if (treeParent instanceof DatabaseObjectTreeObject)
							parentTreeObject = (DatabaseObjectTreeObject)treeParent;
						else
							parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
						
		        		if (parentTreeObject != null) {
    						// New ScEntryHandler statement
		        			ScEntryHandlerStatement scEntryHandlerStatement = new ScEntryHandlerStatement();
		        			
		        			// Set properties
		        			scEntryHandlerStatement.setHandlerResult(scExitHandlerStatement.getHandlerResult());
		        			scEntryHandlerStatement.setHandlerType(scExitHandlerStatement.getHandlerType());
		        			scEntryHandlerStatement.setComment(scExitHandlerStatement.getComment());
		        			scEntryHandlerStatement.setEnable(scExitHandlerStatement.isEnable());
		        			scEntryHandlerStatement.setPreventFromLoops(scExitHandlerStatement.preventFromLoops());
		        			scEntryHandlerStatement.setParent(scExitHandlerStatement.getParent());
		        			scEntryHandlerStatement.setReturnedValue(scExitHandlerStatement.getReturnedValue());
		        			scEntryHandlerStatement.setVersion(scExitHandlerStatement.getVersion());
		        			scEntryHandlerStatement.setNormalizedScreenClassName(scExitHandlerStatement.getNormalizedScreenClassName());
		        			scEntryHandlerStatement.setName("on"+scExitHandlerStatement.getNormalizedScreenClassName()+"Entry");
		        			
		        			// Change status of ScEntryHandler statement
    						scEntryHandlerStatement.bNew = true;
    						scEntryHandlerStatement.hasChanged = true;
    						
    						// Add new ScEntryHandler statement to parent
    						DatabaseObject parentDbo = scEntryHandlerStatement.getParent();
    						parentDbo.add(scEntryHandlerStatement);
    						
    						// Add new ScEntryHandler statement in Tree
    						StatementTreeObject statementTreeObject = new StatementTreeObject(explorerView.viewer, scEntryHandlerStatement);
    						treeParent.addChild(statementTreeObject);

    						// Cut/Paste statements under screen class exit
    						if (selectedPaths.length > 0) {
    							new ClipboardAction(ConvertigoPlugin.clipboardManagerDND).cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT);
	    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManagerDND.objects.length ; i++) {
	    							ConvertigoPlugin.clipboardManagerDND.cutAndPaste(ConvertigoPlugin.clipboardManagerDND.objects[i], statementTreeObject);
	    						}
	    						ConvertigoPlugin.clipboardManagerDND.reset();
    						}
    						
    		   				// Delete ScExitHandler statement
    						scExitHandlerStatement.delete();
    						
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(scEntryHandlerStatement));
		        		}
					}
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change screen class exit handler statement to screen class entry handler statement!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
