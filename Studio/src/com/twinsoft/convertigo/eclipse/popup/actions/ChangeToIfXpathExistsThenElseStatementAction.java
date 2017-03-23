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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsThenElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

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
    			DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) explorerView.getFirstSelectedTreeObject();
    			DatabaseObject databaseObject = treeObject.getObject();
    			// IfXpathExists
    			if ((databaseObject != null) && (databaseObject instanceof IfXpathExistsStatement)) {
    				IfXpathExistsStatement ifStatement = (IfXpathExistsStatement) databaseObject;
										
					DatabaseObjectTreeObject parentTreeObject = treeObject.getOwnerDatabaseObjectTreeObject();
					
	        		if (parentTreeObject != null) {
						// New IfXpathExistsThenElseStatement statement
						IfXpathExistsThenElseStatement ifThenElseStatement = new IfXpathExistsThenElseStatement();
						ifThenElseStatement.bNew = true;
						ifThenElseStatement.hasChanged = true;
						
						// Add new IfThenElseStatement statement to parent
						StatementWithExpressions parentDbo = (StatementWithExpressions) ifStatement.getParent();
						parentDbo.addStatementAfter(ifThenElseStatement, ifStatement);
						
						// Add Then/Else statement
						ThenStatement thenStatement = new ThenStatement();
						thenStatement.bNew = true;
						ifThenElseStatement.addStatement(thenStatement);
						ElseStatement elseStatement = new ElseStatement();
						elseStatement.bNew = true;
						ifThenElseStatement.addStatement(elseStatement);
						
						for (Statement statement: ifStatement.getStatements()) {
							thenStatement.addStatement(statement);
    					}
						
						String name = ifStatement.getName();
						
						// Set properties
						ifThenElseStatement.setCondition(ifStatement.getCondition());
						ifThenElseStatement.setComment(ifStatement.getComment());
						ifThenElseStatement.setEnabled(ifStatement.isEnabled());
						ifThenElseStatement.setVersion(ifStatement.getVersion());
						
		   				// Delete If statement
						ifStatement.delete();
						
						ifThenElseStatement.setName(name);	
						
		   				parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(parentTreeObject.findTreeObjectByUserObject(ifThenElseStatement));
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
