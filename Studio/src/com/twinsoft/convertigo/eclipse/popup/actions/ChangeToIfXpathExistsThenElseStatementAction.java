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
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsThenElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ChangeToIfXpathExistsThenElseStatementAction extends MyAbstractAction {

	public ChangeToIfXpathExistsThenElseStatementAction() {
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
    			// IfXpathExists
    			if ((databaseObject != null) && (databaseObject instanceof IfXpathExistsStatement)) {
    				IfXpathExistsStatement ifStatement = (IfXpathExistsStatement) databaseObject;
					List<Statement> list = ifStatement.getStatements();
					TreePath[] selectedPaths = new TreePath[list.size()];
					for (int i=0; i<list.size(); i++) {
						StatementTreeObject statementTreeObject = (StatementTreeObject) explorerView.findTreeObjectByUserObject(list.get(i));
						selectedPaths[i] = new TreePath(statementTreeObject);
					}
					
					TreeParent treeParent = treeObject.getParent();
					
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
						// New IfXpathExistsThenElseStatement statement
						IfXpathExistsThenElseStatement ifThenElseStatement = new IfXpathExistsThenElseStatement();
						ifThenElseStatement.bNew = true;
						ifThenElseStatement.hasChanged = true;
						
						// Add new IfThenElseStatement statement to parent
						DatabaseObject parentDbo = ifStatement.getParent();
						parentDbo.add(ifThenElseStatement);
						
						// Set correct order
						if (parentDbo instanceof StatementWithExpressions) {
							int index = ((StatementWithExpressions)parentDbo).getOrderedStatements().get(0).indexOf(ifStatement.priority);
		   				    ((StatementWithExpressions)parentDbo).getOrderedStatements().get(0).insertElementAt(ifThenElseStatement.priority, index);
						}
						
						// Set properties
						ifThenElseStatement.setCondition(ifStatement.getCondition());
						ifThenElseStatement.setComment(ifStatement.getComment());
						ifThenElseStatement.setEnable(ifStatement.isEnable());
						ifThenElseStatement.setVersion(ifStatement.getVersion());
						ifThenElseStatement.setXpath(ifStatement.getXpath());
						
						// Add Then/Else statement
						ThenStatement thenStatement = new ThenStatement();
						thenStatement.bNew = true;
						ifThenElseStatement.addStatement(thenStatement);
						ElseStatement elseStatement = new ElseStatement();
						elseStatement.bNew = true;
						ifThenElseStatement.addStatement(elseStatement);
					
						// Add new IfThenElseStep statement in Tree
						StatementTreeObject statementTreeObject = new StatementTreeObject(explorerView.viewer,ifThenElseStatement);
						treeParent.addChild(statementTreeObject);
						StatementTreeObject thenTreeObject = new StatementTreeObject(explorerView.viewer,thenStatement);
						statementTreeObject.addChild(thenTreeObject);
						StatementTreeObject elseTreeObject = new StatementTreeObject(explorerView.viewer,elseStatement);
						statementTreeObject.addChild(elseTreeObject);
						
						// Cut/Paste steps under Then statement
						if (selectedPaths.length > 0) {
							new ClipboardAction(ConvertigoPlugin.clipboardManagerDND).cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManagerDND.objects.length ; i++) {
    							ConvertigoPlugin.clipboardManagerDND.cutAndPaste(ConvertigoPlugin.clipboardManagerDND.objects[i], thenTreeObject);
    						}
    						ConvertigoPlugin.clipboardManagerDND.reset();
						}
						
		   				// Delete If statement
						ifStatement.delete();			

		   				parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifThenElseStatement));
	        		}
				}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change statement to IfThenElse statement!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
