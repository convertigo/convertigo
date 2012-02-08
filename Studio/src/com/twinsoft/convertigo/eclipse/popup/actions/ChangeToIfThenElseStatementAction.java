
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
import com.twinsoft.convertigo.beans.statements.IfStatement;
import com.twinsoft.convertigo.beans.statements.IfThenElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.engine.Engine;

public class ChangeToIfThenElseStatementAction extends MyAbstractAction {

	public ChangeToIfThenElseStatementAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof IfStatement)) {
    				IfStatement ifStatement = (IfStatement) databaseObject;
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
						// New IfThenElseStatement statement
						IfThenElseStatement ifThenElseStatement = new IfThenElseStatement(ifStatement.getCondition());
						ifThenElseStatement.bNew = true;
						ifThenElseStatement.hasChanged = true;
						
						// Add new IfThenElseStatement statement to parent
						DatabaseObject parentDbo = ifStatement.getParent();
						parentDbo.add(ifThenElseStatement);
						
						// Set correct order
						if (parentDbo instanceof StatementWithExpressions) {
							int index = ((StatementWithExpressions)parentDbo).orderedStatements.get(0).indexOf(ifStatement.priority);
		   				    ((StatementWithExpressions)parentDbo).orderedStatements.get(0).insertElementAt(ifThenElseStatement.priority, index);
						}
						
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
    						ClipboardAction.cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManager2.objects.length ; i++) {
    							ConvertigoPlugin.clipboardManager2.cutAndPaste(ConvertigoPlugin.clipboardManager2.objects[i], thenTreeObject);
    						}
    						ConvertigoPlugin.clipboardManager2.reset();
						}
						
		   				// Delete If statement
		   				Engine.theApp.databaseObjectsManager.delete(ifStatement);
		   				parentDbo.remove(ifStatement);		   				

		   				parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(statementTreeObject);
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
