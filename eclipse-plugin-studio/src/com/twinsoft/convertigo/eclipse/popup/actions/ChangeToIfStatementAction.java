
package com.twinsoft.convertigo.eclipse.popup.actions;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.statements.IfStatement;
import com.twinsoft.convertigo.beans.statements.IfThenElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ChangeToIfStatementAction extends MyAbstractAction {

	public ChangeToIfStatementAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof IfThenElseStatement)) {
    				IfThenElseStatement ifThenElseStatement = (IfThenElseStatement) databaseObject;
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
    						// New If statement
		        			IfStatement ifStatement  = new IfStatement(ifThenElseStatement.getCondition());
    						ifStatement.bNew = true;
    						ifStatement.hasChanged = true;
    						
    						// Add new If statement to parent
    						DatabaseObject parentDbo = ifThenElseStatement.getParent();
    						parentDbo.add(ifStatement); 
    						
    						// Set correct order
    						if (parentDbo instanceof StatementWithExpressions) {
    							int index = ((StatementWithExpressions)parentDbo).getOrderedStatements().get(0).indexOf(ifThenElseStatement.priority);
    		   				    ((StatementWithExpressions)parentDbo).getOrderedStatements().get(0).add(index, ifStatement.priority);
    						}
    						
    						// Add new If statement in Tree
    						StatementTreeObject statementTreeObject = new StatementTreeObject(explorerView.viewer,ifStatement);
    						treeParent.addChild(statementTreeObject);

    						// Cut/Paste steps under If statement
    						if (selectedPaths.length > 0) {
    							new ClipboardAction(ConvertigoPlugin.clipboardManagerDND).cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
	    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManagerDND.objects.length ; i++) {
	    							ConvertigoPlugin.clipboardManagerDND.cutAndPaste(ConvertigoPlugin.clipboardManagerDND.objects[i], statementTreeObject);
	    						}
	    						ConvertigoPlugin.clipboardManagerDND.reset();
    						}
    						
    		   				// Delete IfThenElse statement
    		   				ifThenElseStatement.delete();
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifStatement));
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
