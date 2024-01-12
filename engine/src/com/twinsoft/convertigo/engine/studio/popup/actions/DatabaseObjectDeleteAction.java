/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.dialogs.MultipleDeletionDialog;
import com.twinsoft.convertigo.engine.studio.responses.popup.actions.DatabaseObjectDeleteActionResponse;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ConnectorView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SequenceView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;

public class DatabaseObjectDeleteAction extends AbstractRunnableAction {

	private List<WrapDatabaseObject> treeNodesToUpdate;
	private Map<String, Boolean> dboDoDelete;

	public DatabaseObjectDeleteAction(WrapStudio studio) {
		super(studio);
	}

	@Override
	protected void run2() throws Exception {
		String qname = null;
		try {
			treeNodesToUpdate = new ArrayList<WrapDatabaseObject>();
			WrapDatabaseObject treeObject = null;
			WrapObject[] treeObjects = studio.getSelectedObjects().toArray(new WrapObject[0]);
			dboDoDelete = new HashMap<>(treeObjects.length);

			if (treeObjects != null) {
				MultipleDeletionDialog dialog = new MultipleDeletionDialog(studio, "Object Deletion", treeObjects.length > 1);

				for (int i = 0; i < treeObjects.length; ++i) {
					treeObject = (WrapDatabaseObject) treeObjects[i];

					String message = treeObject.instanceOf(Project.class) ?
						java.text.MessageFormat.format("Do you really want to delete the project \"{0}\" and all its sub-objects?", new Object[] {treeObject.getName()}) :
						java.text.MessageFormat.format("Do you really want to delete the object \"{0}\" and all its sub-objects?", new Object[] {treeObject.getName()});

					qname = ((DatabaseObject) treeObject.getObject()).getQName();

					if (!dialog.shouldBeDeleted(message)) {
						dboDoDelete.put(qname, false);
						continue;
					}
					dboDoDelete.put(qname, true);

					if (treeObject.instanceOf(Project.class)) {
						((ProjectView) treeObject).closeAllEditors();
					}
					else if (treeObject.instanceOf(Sequence.class)) {
					    ((ProjectView) ((SequenceView) treeObject)/*.getParent()*/.getParent()).closeSequenceEditors((Sequence) treeObject.getObject());
					}
					else if (treeObject.instanceOf(Connector.class)) {
					    ((ProjectView) ((ConnectorView) treeObject)/*.getParent()*/.getParent()).closeConnectorEditors((Connector) treeObject.getObject());
		        	}
					else if (treeObject.instanceOf(Step.class)) {
					//    						// We close the editor linked with the SimpleStep (=SequenceJsStep)
					//    						if (treeObject.getObject() instanceof SimpleStep) {
					//    							boolean find = false;
					//    							SimpleStep simpleStep = (SimpleStep) treeObject.getObject();
					//    							IWorkbenchPage page = this.getActivePage();	
					//    							IEditorReference[] editors = page.getEditorReferences();
					//    							int _i = 0;
					//    							while (find != true && _i < editors.length) {
					//    								IEditorReference editor = editors[_i];
					//    								IEditorPart editorPart = page.findEditor(editor.getEditorInput());
					//    								if (editorPart != null && editorPart instanceof JscriptStepEditor) {
					//    									JscriptStepEditor jscriptEditor = (JscriptStepEditor) editorPart;
					//    									if (jscriptEditor.getSimpleStepLinked().equals(simpleStep)) {
					// 		    							   find = true;
					//		    							   page.activate(editorPart);
					//		    							   page.closeEditor(editorPart, false);
					//    									}
					//    								}
					//    								++_i;
					//    							}
					}

					delete(treeObject);

					if (treeObject.instanceOf(Project.class)) {
						//     		explorerView.removeProjectTreeObject(treeObject);
		        	}
					else {
					//    						// prevents treeObject and its childs to receive further TreeObjectEvents
					//    						if (treeObject instanceof TreeObjectListener)
					//    							explorerView.removeTreeObjectListener(treeObject);
					//    						treeObject.removeAllChildren();
					}
					//    					
					//    					explorerView.fireTreeObjectRemoved(new TreeObjectEvent(treeObject));
				}
			}

			// Updating the tree and the properties panel
			Enumeration<WrapDatabaseObject> enumeration = Collections.enumeration(treeNodesToUpdate);
			WrapDatabaseObject parentTreeObject;
			while (enumeration.hasMoreElements()) {
				parentTreeObject = enumeration.nextElement();
				if (parentTreeObject != null) {
//					explorerView.reloadTreeObject(parentTreeObject);
//					explorerView.setSelectedTreeObject(parentTreeObject);
				}
			}
			//    				
			// Refresh tree to show potential 'broken' steps
			//    				explorerView.refreshTree();
		}
		catch (Exception e) {
			throw e;
		}
	}

	private void delete(WrapDatabaseObject treeObject) throws ConvertigoException, IOException {
		WrapDatabaseObject parentTreeObject = null;
		WrapObject treeParent = treeObject.getParent();

		DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
		DatabaseObject parent = databaseObject.getParent();

		while ((treeParent != null) && (!(treeParent instanceof WrapDatabaseObject))) {
			treeParent = treeParent.getParent();
		}

		if (treeParent != null) {
			parentTreeObject = (WrapDatabaseObject) treeParent;
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

	private void delete(DatabaseObject databaseObject) throws EngineException, IOException {
		if (databaseObject instanceof Connector) {
			if (((Connector) databaseObject).isDefault) {
				throw new EngineException("Cannot delete the default connector!");
			}

			String projectName = databaseObject.getParentName();
			deleteResourcesFolder(projectName, "soap-templates", databaseObject.getName());
			deleteResourcesFolder(projectName, "Traces", databaseObject.getName());
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
//				MessageBox messageBox = new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
//				messageBox.setMessage("Do you want to delete the whole resource folder \"" + mobilePlatform.getRelativeResourcePath() + "\"?"); 
//				messageBox.setText("Delete the \""+resourceFolder.getName()+"\" folder?");
//				if (messageBox.open() == SWT.YES) {
//					FileUtils.deleteQuietly(resourceFolder);
//				}
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
//			ConvertigoPlugin.getDefault().deleteProjectPluginResource(databaseObject.getName());
		}
		else {
			databaseObject.delete();
		}

		if (databaseObject instanceof CouchDbConnector) {
			CouchDbConnector couchDbConnector = (CouchDbConnector)databaseObject;
			String db = couchDbConnector.getDatabaseName();
			if (!db.isEmpty()) {
//				MessageBox messageBox = new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
//				messageBox.setMessage("Do you want to delete the \""+db+"\" database from the CouchDb server?"); 
//				messageBox.setText("Delete the database?");
//				if (messageBox.open() == SWT.YES) {
//					couchDbConnector.getCouchClient().deleteDatabase(db);
//				}
			}
		}

//		ConvertigoPlugin.logDebug("The object \"" + databaseObject.getQName() + "\" has been deleted from the database repository!");
    }

	private void deleteResourcesFolder(String projectName, String resourcesFolder, String dboName) throws IOException {
		// Delete soap templates for this connector
		String dirPath = Engine.projectDir(projectName) + "/" + resourcesFolder + "/" + dboName;
		File dir = new File(dirPath);
		if (dir.exists()) {
			String[] buttons = { "Yes", "No" };
			int response = studio.openMessageBox(
					"Also delete linked resources?",
					"Some resources are linked to the deleted connector.\n\n" + "Do you also want to delete folder:\n\n\"" + dirPath + "\"",
					buttons
			);
			if (response == 0) {			
				try {
					DatabaseObjectsManager.deleteDir(dir);
				} catch (IOException e) {
				    throw e;
					//ConvertigoPlugin.logDebug("Unable to delete directory \""+ dirPath+"\"!");
				}
			}
		}
	}

	@Override
	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		Element response = super.toXml(document, qname);
		if (response != null) {
			return response;
		}

		return new DatabaseObjectDeleteActionResponse(dboDoDelete.get(qname)).toXml(document, qname);
	}
}
