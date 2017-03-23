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
import com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsThenElseStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

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
    			DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) explorerView.getFirstSelectedTreeObject();
    			DatabaseObject databaseObject = treeObject.getObject();
    			if ((databaseObject != null) && (databaseObject instanceof IfXpathExistsThenElseStatement)) {
    				IfXpathExistsThenElseStatement ifThenElseStatement = (IfXpathExistsThenElseStatement) databaseObject;
    				// IfXpathExistsThenElse statement
    				if (ifThenElseStatement.hasThenElseStatements()) {
						DatabaseObjectTreeObject parentTreeObject = treeObject.getOwnerDatabaseObjectTreeObject();
						
		        		if (parentTreeObject != null) {
    						// New IfXpathExistsStatement statement
		        			IfXpathExistsStatement ifStatement  = new IfXpathExistsStatement();
    						ifStatement.bNew = true;
    						ifStatement.hasChanged = true;
    						
    						// Add new If statement to parent
    						StatementWithExpressions parentDbo = (StatementWithExpressions) ifThenElseStatement.getParent();
    						parentDbo.addStatementAfter(ifStatement, ifThenElseStatement); 
    						
        					for (Statement statement: ifThenElseStatement.getThenStatement().getStatements()) {
        						ifStatement.addStatement(statement);
        					}
    						
    						// Set properties
    						ifStatement.setCondition(ifThenElseStatement.getCondition());
    						ifStatement.setComment(ifThenElseStatement.getComment());
    						ifStatement.setEnabled(ifThenElseStatement.isEnabled());
    						ifStatement.setVersion(ifThenElseStatement.getVersion());
    						
    						String name = ifThenElseStatement.getName();
    						
    		   				// Delete IfThenElse statement
    						ifThenElseStatement.delete();
    						
    						ifStatement.setName(name);
    						
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(parentTreeObject.findTreeObjectByUserObject(ifStatement));
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
