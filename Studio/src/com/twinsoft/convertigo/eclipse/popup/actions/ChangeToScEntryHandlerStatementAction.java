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
    			DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) explorerView.getFirstSelectedTreeObject();
    			DatabaseObject databaseObject = treeObject.getObject();
    			// For ScExitHandler statement
    			if ((databaseObject != null) && (databaseObject instanceof ScExitHandlerStatement || databaseObject instanceof ScExitDefaultHandlerStatement)) {
    				HandlerStatement scExitHandlerStatement = (HandlerStatement) databaseObject;
    				
					DatabaseObjectTreeObject parentTreeObject = treeObject.getOwnerDatabaseObjectTreeObject();
					
	        		if (parentTreeObject != null) {
						// New ScEntryHandler statement
	        			HandlerStatement scEntryHandlerStatement = databaseObject instanceof ScExitHandlerStatement ?
	        					new ScEntryHandlerStatement() : new ScEntryDefaultHandlerStatement();
	        			
	        			// Set properties
	        			String handlerResult = scExitHandlerStatement.getHandlerResult();
	        			if (ScHandlerStatement.RETURN_ACCUMULATE.equals(handlerResult)) {
	        				handlerResult = ScHandlerStatement.RETURN_REDETECT;
	        			}
	        			
	        			scEntryHandlerStatement.setHandlerResult(handlerResult);
	        			scEntryHandlerStatement.setComment(scExitHandlerStatement.getComment());
	        			scEntryHandlerStatement.setEnabled(scExitHandlerStatement.isEnabled());
	        			scEntryHandlerStatement.setPreventFromLoops(scExitHandlerStatement.preventFromLoops());
	        			scEntryHandlerStatement.setParent(scExitHandlerStatement.getParent());
	        			scEntryHandlerStatement.setReturnedValue(scExitHandlerStatement.getReturnedValue());
	        			scEntryHandlerStatement.setVersion(scExitHandlerStatement.getVersion());
	        			
	        			if (databaseObject instanceof ScExitHandlerStatement) {
	        				ScExitHandlerStatement scExit = (ScExitHandlerStatement) scExitHandlerStatement;
	        				ScEntryHandlerStatement scEntry = (ScEntryHandlerStatement) scEntryHandlerStatement;
	        				
	        				scEntry.setNormalizedScreenClassName(scExit.getNormalizedScreenClassName());
	        				scEntry.setName("on" + scExit.getNormalizedScreenClassName() + "Entry");
	        			}
	        			
	        			// Change status of ScEntryHandler statement
						scEntryHandlerStatement.bNew = true;
						scEntryHandlerStatement.hasChanged = true;
						
						// Add new ScEntryHandler statement to parent
						DatabaseObject parentDbo = scEntryHandlerStatement.getParent();
						parentDbo.add(scEntryHandlerStatement);
						
						for (Statement statement: scExitHandlerStatement.getStatements()) {
	    					scEntryHandlerStatement.addStatement(statement);
	    				}
						
		   				// Delete ScExitHandler statement
						scExitHandlerStatement.delete();
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                DatabaseObjectTreeObject newTreeObject = parentTreeObject.findDatabaseObjectTreeObjectChild(scEntryHandlerStatement);
		                explorerView.setSelectedTreeObject(newTreeObject);
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
