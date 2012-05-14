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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsThenElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.engine.Engine;

public class ChangeToIfXpathExistsStatementAction extends MyAbstractAction {

	public ChangeToIfXpathExistsStatementAction() {
	}

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
    			if ((databaseObject != null) && (databaseObject instanceof IfXpathExistsThenElseStatement)) {
    				IfXpathExistsThenElseStatement ifThenElseStatement = (IfXpathExistsThenElseStatement) databaseObject;
    				if (ifThenElseStatement.hasThenElseStatements()) {
    					ThenStatement thenStatement = ifThenElseStatement.getThenStatement();
    					List<Statement> list = thenStatement.getStatements();
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
    						// New IfXpathExistsStatement statement
		        			IfXpathExistsStatement ifStatement  = new IfXpathExistsStatement(ifThenElseStatement.getCondition());
    						ifStatement.bNew = true;
    						ifStatement.hasChanged = true;
    						
    						// Add new If statement to parent
    						DatabaseObject parentDbo = ifThenElseStatement.getParent();
    						parentDbo.add(ifStatement); 
    						
    						// Set correct order
    						if (parentDbo instanceof StatementWithExpressions) {
    							int index = ((StatementWithExpressions)parentDbo).orderedStatements.get(0).indexOf(ifThenElseStatement.priority);
    		   				    ((StatementWithExpressions)parentDbo).orderedStatements.get(0).insertElementAt(ifStatement.priority, index);
    						}
    						
    						// Add new If statement in Tree
    						StatementTreeObject statementTreeObject = new StatementTreeObject(explorerView.viewer,ifStatement);
    						treeParent.addChild(statementTreeObject);

    						// Cut/Paste steps under If statement
    						if (selectedPaths.length > 0) {
	    						ClipboardAction.cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
	    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManager2.objects.length ; i++) {
	    							ConvertigoPlugin.clipboardManager2.cutAndPaste(ConvertigoPlugin.clipboardManager2.objects[i], statementTreeObject);
	    						}
	    						ConvertigoPlugin.clipboardManager2.reset();
    						}
    						
    		   				// Delete IfThenElse statement
    		   				Engine.theApp.databaseObjectsManager.delete(ifThenElseStatement);
    		   				parentDbo.remove(ifThenElseStatement);
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(statementTreeObject);
		        		}
    				}
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change statement to If statement!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
