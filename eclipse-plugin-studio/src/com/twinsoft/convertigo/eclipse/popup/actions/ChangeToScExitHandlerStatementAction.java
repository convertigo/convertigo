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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/7.1.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToScExitHandlerStatementAction.java $
 * $Author: julienda $
 * $Revision: 34593 $
 * $Date: 2013-07-25 17:22:06 +0200 (ven., 25 juil. 2014) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;

public class ChangeToScExitHandlerStatementAction extends MyAbstractAction {

	public ChangeToScExitHandlerStatementAction() { }

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
    			// For ScEntryHandler statement
    			if ((databaseObject != null) && (databaseObject instanceof ScEntryHandlerStatement || databaseObject instanceof ScEntryDefaultHandlerStatement)) {
    				HandlerStatement scEntryHandlerStatement = (HandlerStatement) databaseObject;
    				
					DatabaseObjectTreeObject parentTreeObject = treeObject.getOwnerDatabaseObjectTreeObject();
					
	        		if (parentTreeObject != null) {
						// New ScExitHandler statement
	        			HandlerStatement scExitHandlerStatement = databaseObject instanceof ScEntryHandlerStatement ?
	        					new ScExitHandlerStatement() : new ScExitDefaultHandlerStatement();
	        			
	        			// Set properties
	        			String handlerResult = scEntryHandlerStatement.getHandlerResult();
	        			if (ScHandlerStatement.RETURN_REDETECT.equals(handlerResult)) {
	        				handlerResult = ScHandlerStatement.RETURN_ACCUMULATE;
	        			} else if (ScHandlerStatement.RETURN_SKIP.equals(handlerResult)) {
	        				handlerResult = "";
	        			}
	        			
	        			scExitHandlerStatement.setHandlerResult(handlerResult);
	        			scExitHandlerStatement.setComment(scEntryHandlerStatement.getComment());
	        			scExitHandlerStatement.setEnabled(scEntryHandlerStatement.isEnabled());
	        			scExitHandlerStatement.setPreventFromLoops(scEntryHandlerStatement.preventFromLoops());
	        			scExitHandlerStatement.setParent(scEntryHandlerStatement.getParent());
	        			scExitHandlerStatement.setReturnedValue(scEntryHandlerStatement.getReturnedValue());
	        			scExitHandlerStatement.setVersion(scEntryHandlerStatement.getVersion());
	        			
	        			if (databaseObject instanceof ScEntryHandlerStatement) {
	        				ScExitHandlerStatement scExit = (ScExitHandlerStatement) scExitHandlerStatement;
	        				ScEntryHandlerStatement scEntry = (ScEntryHandlerStatement) scEntryHandlerStatement;
	        				
	        				scExit.setNormalizedScreenClassName(scEntry.getNormalizedScreenClassName());
	        				scExit.setName("on" + scEntry.getNormalizedScreenClassName() + "Exit");
	        			}
	        			
	        			// Change status of scExitHanlder statement
						scExitHandlerStatement.bNew = true;
						scExitHandlerStatement.hasChanged = true;
						
						// Add new ScExitHandler statement to parent
						DatabaseObject parentDbo = scEntryHandlerStatement.getParent();
						parentDbo.add(scExitHandlerStatement);
						
						// Add new ScExitHandler statement in Tree
						StatementTreeObject statementTreeObject = new StatementTreeObject(explorerView.viewer, scExitHandlerStatement);
						parentTreeObject.addChild(statementTreeObject);
						
						for (Statement statement: scEntryHandlerStatement.getStatements()) {
							scExitHandlerStatement.addStatement(statement);
	    				}
						
		   				// Delete ScEntryHandler statement
						scEntryHandlerStatement.delete();
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                DatabaseObjectTreeObject newTreeObject = parentTreeObject.findDatabaseObjectTreeObjectChild(scExitHandlerStatement);
		                explorerView.setSelectedTreeObject(newTreeObject);
	        		}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change screen class entry handler statement to screen class exit handler statement!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
