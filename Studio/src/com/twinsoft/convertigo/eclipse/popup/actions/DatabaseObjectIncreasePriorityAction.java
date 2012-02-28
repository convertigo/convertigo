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

import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;

public class DatabaseObjectIncreasePriorityAction extends MyAbstractAction implements IViewActionDelegate {
	
	private Vector<TreeParent> treeNodesToUpdate;
	private int counter = 1;
	
	public DatabaseObjectIncreasePriorityAction() {
		super();
	}

	public DatabaseObjectIncreasePriorityAction(int counter) {
		super();
		this.counter = counter;
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	treeNodesToUpdate = new Vector<TreeParent>();
        	
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			String[] selectedPaths = new String[treeObjects.length];
    			if (treeObjects != null) {
    				// Increase priority
    				TreeObject treeObject = null;
    				for (int i = 0 ; i < treeObjects.length ; i++) {
    					treeObject = treeObjects[i];
    					selectedPaths[i] = treeObject.getPath();
   						increasePriority(treeObject);
    				}
    				
    				// Updating the tree and the properties panel
    				Enumeration<TreeParent> enumeration = treeNodesToUpdate.elements();
    				TreeParent parentTreeObject = null;
    				while (enumeration.hasMoreElements()) {
    					parentTreeObject = enumeration.nextElement();
    					explorerView.reloadTreeObject(parentTreeObject);
    				}
    				
    				// Restore selection
    	    		TreeObjectEvent treeObjectEvent;
    	        	for (int i=0; i<selectedPaths.length; i++) {
    	        		String previousPath = selectedPaths[i];
    	        		treeObject = explorerView.findTreeObjectByPath(parentTreeObject, previousPath);
    	        		if (treeObject != null) {
    	        			treeObjects[i] = treeObject;
    		                treeObjectEvent = new TreeObjectEvent(treeObject);
    		                explorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
    	        		}
    	        	}
    				explorerView.setSelectedTreeObjects(treeObjects);
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to increase priority!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	private void increasePriority(TreeObject treeObject) throws EngineException {
		int count = counter;
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
			DatabaseObject parent = databaseObject.getParent();
			
			while (count-->0) {
				if (parent instanceof ScreenClass) {
					((ScreenClass)parent).increasePriority(databaseObject);
				}
				if (parent instanceof Sequence) {
					((Sequence)parent).increasePriority(databaseObject);
				}
				if (parent instanceof TransactionWithVariables) {
					((TransactionWithVariables)parent).increasePriority(databaseObject);
				}
				if (parent instanceof StatementWithExpressions) {
					((StatementWithExpressions)parent).increasePriority(databaseObject);
				}
				if (parent instanceof HTTPStatement) {
					((HTTPStatement)parent).increasePriority(databaseObject);
				}
				if (parent instanceof StepWithExpressions) {
					((StepWithExpressions)parent).increasePriority(databaseObject);
				}
				if (parent instanceof RequestableStep) {
					((RequestableStep)parent).increasePriority(databaseObject);
				}
				if (parent instanceof TestCase) {
					((TestCase)parent).increasePriority(databaseObject);
				}
			}
			
			if (parent.hasChanged) {
				TreeParent parentTreeObject = null;
				TreeParent treeParent = treeObject.getParent();
				if (treeParent instanceof FolderTreeObject)
					parentTreeObject = treeObject.getParent().getParent();
				else
					parentTreeObject = treeParent;
				
				if (!treeNodesToUpdate.contains(parentTreeObject)) {
					treeNodesToUpdate.add(parentTreeObject);
				}
			}
		}
		else {
			DatabaseObjectTreeObject databaseObjectTreeObject = null;
			if (treeObject instanceof PropertyTableRowTreeObject) {
				PropertyTableTreeObject propertyTableTreeObject = (PropertyTableTreeObject)treeObject.getParent();
				while (count-->0) {
					if ((treeObject = propertyTableTreeObject.moveRow((PropertyTableRowTreeObject)treeObject, true)) != null) {
						databaseObjectTreeObject = (DatabaseObjectTreeObject)propertyTableTreeObject.getParent();
					}
				}
			}
			else if (treeObject instanceof PropertyTableColumnTreeObject) {
				PropertyTableRowTreeObject propertyTableRowTreeObject = (PropertyTableRowTreeObject)treeObject.getParent();
				PropertyTableTreeObject propertyTableTreeObject = (PropertyTableTreeObject)propertyTableRowTreeObject.getParent();
				while (count-->0) {
					if ((treeObject = propertyTableRowTreeObject.moveColumn((PropertyTableColumnTreeObject)treeObject, true)) != null) {
						databaseObjectTreeObject = (DatabaseObjectTreeObject)propertyTableTreeObject.getParent();
					}
				}
			}
			
			if (databaseObjectTreeObject != null) {
				if (databaseObjectTreeObject.hasChanged()) {
					DatabaseObjectTreeObject parentTreeObject = null;
					TreeParent treeParent = databaseObjectTreeObject.getParent();
					if (treeParent instanceof FolderTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					
					if (!treeNodesToUpdate.contains(parentTreeObject)) {
						treeNodesToUpdate.add(parentTreeObject);
					}
				}
			}
			
		}
	}

	public void init(IViewPart view) {
	}
}
