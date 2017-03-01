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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.MultipleDeletionDialog;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class DatabaseObjectDeleteAction extends MyAbstractAction {

	private List<DatabaseObjectTreeObject> treeNodesToUpdate;
	
	public DatabaseObjectDeleteAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	treeNodesToUpdate = new ArrayList<DatabaseObjectTreeObject>();
        	
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			if (treeObjects != null) {
    				DatabaseObjectTreeObject treeObject = null;
    				String message = "";
    				MultipleDeletionDialog dialog;
    				
    				if (treeObjects.length == 1) {
    					dialog = new MultipleDeletionDialog(shell, "Object Deletion", false);
    				}
    				else {
    					dialog = new MultipleDeletionDialog(shell, "Object Deletion", true);
    				}
    				 
    				int len;
    				
    				len = treeObjects.length;
    				for (int i = 0 ; i < len ; i++) {
    					treeObject = (DatabaseObjectTreeObject) treeObjects[i];
    		        	
    					if (treeObject instanceof ProjectTreeObject) {
        					message = java.text.MessageFormat.format("Do you really want to delete the project \"{0}\" and all its sub-objects?", new Object[] {treeObject.getName()});
    					}
    					else {
        					message = java.text.MessageFormat.format("Do you really want to delete the object \"{0}\" and all its sub-objects?", new Object[] {treeObject.getName()});
    					}	
    					
    		        	if (!dialog.shouldBeDeleted(message))
    		        		continue;
    		        	
    					if (treeObject instanceof ProjectTreeObject) {
    						((ProjectTreeObject)treeObject).closeAllEditors();
    		        	}
    					else if (treeObject instanceof SequenceTreeObject) {
    						((ProjectTreeObject) ((SequenceTreeObject) treeObject).getParent().getParent()).closeSequenceEditors((Sequence)treeObject.getObject());
    		        	}
    					else if (treeObject instanceof ConnectorTreeObject) {
    						((ProjectTreeObject) ((ConnectorTreeObject) treeObject).getParent().getParent()).closeConnectorEditors((Connector)treeObject.getObject());
    		        	}
    					else if (treeObject instanceof StepTreeObject) {
    						// We close the editor linked with the SimpleStep (=SequenceJsStep)
    						if ( treeObject.getObject() instanceof SimpleStep) {
    							boolean find = false;
    							SimpleStep simpleStep = (SimpleStep) treeObject.getObject();
    							IWorkbenchPage page = this.getActivePage();	
    							IEditorReference[] editors = page.getEditorReferences();
    							int _i = 0;
    							while (find != true && _i < editors.length) {
    								IEditorReference editor = editors[_i];
    								IEditorPart editorPart = page.findEditor(editor.getEditorInput());
    								if (editorPart != null && editorPart instanceof JscriptStepEditor) {
    									JscriptStepEditor jscriptEditor = (JscriptStepEditor) editorPart;
    									if (jscriptEditor.getSimpleStepLinked().equals(simpleStep)) {
 		    							   find = true;
		    							   page.activate(editorPart);
		    							   page.closeEditor(editorPart, false);
    									}
    								}
    								++_i;
    							}
    						}
    					}
    					
    					delete(treeObject);
    		        	
    					if (treeObject instanceof ProjectTreeObject) {
    		        		explorerView.removeProjectTreeObject(treeObject);
    		        	}
    					else {
    						// prevents treeObject and its childs to receive further TreeObjectEvents
    						if (treeObject instanceof TreeObjectListener)
    							explorerView.removeTreeObjectListener(treeObject);
    						treeObject.removeAllChildren();
    					}
    					
    					explorerView.fireTreeObjectRemoved(new TreeObjectEvent(treeObject));
    				}
    				
    				// Updating the tree and the properties panel
    				Enumeration<DatabaseObjectTreeObject> enumeration = Collections.enumeration(treeNodesToUpdate);
    				DatabaseObjectTreeObject parentTreeObject;
    				while (enumeration.hasMoreElements()) {
    					parentTreeObject = enumeration.nextElement();
    					if (parentTreeObject != null) {
        					explorerView.reloadTreeObject(parentTreeObject);
        					explorerView.setSelectedTreeObject(parentTreeObject);
    					}
    				}
    				
    				// Refresh tree to show potential 'broken' steps
    				explorerView.refreshTree();
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to delete object!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	private void delete(DatabaseObjectTreeObject treeObject) throws CoreException, ConvertigoException {
		
		DatabaseObjectTreeObject parentTreeObject = null;
		TreeParent treeParent = treeObject.getParent();
		
		DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
		DatabaseObject parent = databaseObject.getParent();
		
		while ((treeParent != null) && (!(treeParent instanceof DatabaseObjectTreeObject))) {
			treeParent = treeParent.getParent();
		}
		
		if (treeParent != null) {
			parentTreeObject = (DatabaseObjectTreeObject) treeParent;
		}
		
		delete(databaseObject);
		
		/*if ((parent != null) && (!parent.hasChanged))
			ConvertigoPlugin.projectManager.save(parent, false);*/
		
		// Do not save after a deletion anymore
		if (parent != null) {
			parentTreeObject.hasBeenModified(true);
		}
				
		if ((parentTreeObject != null) && !treeNodesToUpdate.contains(parentTreeObject)) {
			treeNodesToUpdate.add(parentTreeObject);
		}
		
	}
	
	private void delete(DatabaseObject databaseObject) throws EngineException, CoreException {
		
		if (databaseObject instanceof Connector) {
			if (((Connector) databaseObject).isDefault) {
				throw new EngineException("Cannot delete the default connector!");
			}
			
			String dirPath, projectName;
			File dir;
			
			projectName = databaseObject.getParent().getName();
			
			MessageBox messageBox = new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setText("Also delete linked resources?");
			
			// Delete soap templates for this connector
			dirPath = Engine.PROJECTS_PATH + "/"+ projectName + "/soap-templates/" + databaseObject.getName();
			dir = new File(dirPath);
			if (dir.exists()) {
				messageBox.setMessage("Some resources are linked to the deleted connector.\n\n" +
						"Do you also want to delete folder:\n\n\""+dirPath+"\""); 
				if (messageBox.open() == SWT.YES) {			
					try {
						DatabaseObjectsManager.deleteDir(dir);
					} catch (IOException e) {
						ConvertigoPlugin.logDebug("Unable to delete directory \""+ dirPath+"\"!");
					}
				}
			}
			
			// Delete directory corresponding to connector under Traces directory
			dirPath = Engine.PROJECTS_PATH + "/"+ projectName + "/Traces/" + databaseObject.getName();
			dir = new File(dirPath);
			if (dir.exists()) {
				messageBox.setMessage("Some resources are linked to the deleted connector.\n\n" +
						"Do you also want to delete folder:\n\n\""+dirPath+"\""); 
				
				if (messageBox.open() == SWT.YES) {		
					try {
						DatabaseObjectsManager.deleteDir(dir);
					} catch (IOException e) {
						ConvertigoPlugin.logDebug("Unable to delete directory \""+ dirPath+"\"!");
					}
				}
			}

		}
		else if (databaseObject instanceof Transaction) {
			if (((Transaction) databaseObject).isDefault) {
				throw new EngineException("Cannot delete the default transaction!");
			}
		}
		else if (databaseObject instanceof ScreenClass) {
			if ((databaseObject.getParent()) instanceof Project) {
				throw new EngineException("Cannot delete the root screen class!");
			}
		}
		else if (databaseObject instanceof Statement) {
			if ((databaseObject instanceof ThenStatement) ||
				(databaseObject instanceof ElseStatement)) {
				throw new EngineException("Cannot delete this statement!");
			}
		}
		else if (databaseObject instanceof Step) {
			if ((databaseObject instanceof ThenStep) ||
				(databaseObject instanceof ElseStep)) {
				throw new EngineException("Cannot delete this step!");
			}
		}
		else if (databaseObject instanceof MobilePlatform) {
			MobilePlatform mobilePlatform = (MobilePlatform) databaseObject;
			File resourceFolder = mobilePlatform.getResourceFolder();
			if (resourceFolder.exists()) {
				MessageBox messageBox = new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage("Do you want to delete the whole resource folder \"" + mobilePlatform.getRelativeResourcePath() + "\"?"); 
				messageBox.setText("Delete the \""+resourceFolder.getName()+"\" folder?");
				if (messageBox.open() == SWT.YES) {
					FileUtils.deleteQuietly(resourceFolder);
				}
			}
		}
		else if (databaseObject instanceof PageComponent) {
			if (((PageComponent) databaseObject).isRoot) {
				throw new EngineException("Cannot delete the root page!");
			}
		}
		
		if (databaseObject instanceof Project) {
			// Deleted project will be backup, car will be deleted to avoid its deployment at engine restart
			//Engine.theApp.databaseObjectsManager.deleteProject(databaseObject.getName());
			Engine.theApp.databaseObjectsManager.deleteProjectAndCar(databaseObject.getName());
			ConvertigoPlugin.getDefault().deleteProjectPluginResource(databaseObject.getName());
		}
		else {
			databaseObject.delete();
		}
		
		if (databaseObject instanceof CouchDbConnector) {
			CouchDbConnector couchDbConnector = (CouchDbConnector)databaseObject;
			String db = couchDbConnector.getDatabaseName();
			if (!db.isEmpty()) {
				MessageBox messageBox = new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage("Do you want to delete the \""+db+"\" database from the CouchDb server?"); 
				messageBox.setText("Delete the database?");
				if (messageBox.open() == SWT.YES) {
					couchDbConnector.getCouchClient().deleteDatabase(db);
				}
			}
		}
		
		ConvertigoPlugin.logDebug("The object \"" + databaseObject.getQName() + "\" has been deleted from the database repository!");
    }
	
	//TODO : add DeleteEdit class
}
