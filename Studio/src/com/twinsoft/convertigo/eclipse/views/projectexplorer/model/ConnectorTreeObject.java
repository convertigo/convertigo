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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.Replacement;

public class ConnectorTreeObject extends DatabaseObjectTreeObject {

	private boolean byDefault = false;
	
	public ConnectorTreeObject(Viewer viewer, Connector object) {
		this(viewer, object, false);
	}
	
	public ConnectorTreeObject(Viewer viewer, Connector object, boolean inherited) {
		super(viewer, object, inherited);
		isDefault = getObject().isDefault;
	}
	
	@Override
	public Connector getObject(){
		return (Connector) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isDefault")) {
			byDefault = getObject().isDefault;
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(byDefault));
		}
		return super.testAttribute(target, name, value);
	}

	public void launchEditor() {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Open editor
			openConnectorEditor();

		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void openConnectorEditor() {
		Connector connector = getObject();
		synchronized (connector) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = getConnectorEditor(activePage, connector);
				
				if (editorPart != null)
					activePage.activate(editorPart);
				else {
					try {
						editorPart = activePage.openEditor(new ConnectorEditorInput(connector),
										"com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor");
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the connector editor '"
										+ connector.getName() + "'");
					}
				}
			}
		}
	}
	
	private IEditorPart getConnectorEditor(IWorkbenchPage activePage, Connector connector) {
		IEditorPart editorPart = null;
		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					}
					catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}

	@Override
	public boolean rename(String newName, boolean dialog) {
		if (super.rename(newName, dialog)) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = getConnectorEditor(activePage, getObject());
				// If editor was open, close it and reopen it
				if (editorPart != null) {
					activePage.closeEditor(editorPart, false);
					openConnectorEditor();
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)) && (treeObject.getParents().contains(this))) {
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
				
				Connector connector = this.getObject();
				
				// A transaction has been removed
				if (databaseObject instanceof Transaction) {
					if (connector.getEndTransactionName().equals(databaseObject.getName())) {
						connector.setEndTransactionName("");
						
	    		    	try {
	    					ConvertigoPlugin.getDefault().getProjectExplorerView().refreshTreeObject(this);
	    				} catch (Exception e) {
	    					ConvertigoPlugin.logWarning(e, "Could not refresh in tree Connector \""+databaseObject.getName()+"\" !");
	    				}
						
					}
				}
			}
		}
	}
	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "":propertyName);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			// If a bean name has changed
			if (propertyName.equals("name")) {
				handlesBeanNameChanged(treeObjectEvent);
			}
			else {
				// if this connector has changed
				if (treeObject.equals(this)) {
					Connector connector = this.getObject();
					
					if (connector instanceof SapJcoConnector) {
						try {
							((SapJcoConnector)connector).getSapJCoProvider().updateDestination();
						}
						catch (Exception e) {
							ConvertigoPlugin.logWarning(e, "Could not update SAP destination !");
						}
					}
					else if (connector instanceof CouchDbConnector) {
						if (propertyName.equals("https") ||
							propertyName.equals("port") ||
							propertyName.equals("server") ||
							propertyName.equals("couchUsername") ||
							propertyName.equals("couchPassword"))
						{
							((CouchDbConnector)connector).release();
							CouchDbManager.syncDocument(connector);
		    		    	try {
		    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
		    				} catch (Exception e) {
		    					ConvertigoPlugin.logWarning(e, "Could not reload connector \""+connector.getName()+"\" in tree !");
		    				}
						}
						else if (propertyName.equals("databaseName")) {
							CouchDbManager.syncDocument(connector);
		    		    	try {
		    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
		    				} catch (Exception e) {
		    					ConvertigoPlugin.logWarning(e, "Could not reload connector \""+connector.getName()+"\" in tree !");
		    				}
						}
					}
				}
			}
		}
	}

	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;

		Connector connector = this.getObject();
		
		if (databaseObject instanceof Transaction) {
			Transaction transaction = (Transaction)databaseObject;
			if (transaction.getConnector().equals(connector)) {
				if (connector.getEndTransactionName().equals(oldValue)) {
					connector.setEndTransactionName((String)newValue);
					
    		    	try {
    					ConvertigoPlugin.getDefault().getProjectExplorerView().refreshTreeObject(this);
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not refresh in tree Connector \""+databaseObject.getName()+"\" !");
    				}
					
				}
			}
		}
		
		// Case of this connector rename : update all transaction schemas
		if (treeObject.equals(this)) {
			String path = Project.XSD_FOLDER_NAME +"/"
						+ Project.XSD_INTERNAL_FOLDER_NAME;
			
			String oldPath = path + "/" + (String)oldValue;
			String newPath = path + "/" + (String)newValue;
			
			IFolder folder = getProjectTreeObject().getFolder(oldPath);
			if (folder.exists()) {
				try {
					// rename folder (xsd/internal/connector)
					folder.move(new Path((String)newValue), true, null);
					
					// make replacements in schema file
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement((String)oldValue+"__", (String)newValue+"__"));
					IFolder newFolder = folder.getParent().getFolder(new Path((String)newValue));
					for (Transaction transaction : getObject().getTransactionsList()) {
						IFile file = newFolder.getFile(new Path(transaction.getName()+".xsd"));
						if (file.exists()) {
							String filePath = file.getLocation().makeAbsolute().toString();
							try {
								ProjectUtils.makeReplacementsInFile(replacements, filePath);
							} catch (Exception e) {
								ConvertigoPlugin.logWarning(e, "Could rename \""+oldValue+"\" to \""+newValue+"\" in schema file \""+filePath+"\" !");
							}
						}
					}
					
					// refresh folder
					folder.refreshLocal(IResource.DEPTH_ONE, null);
					
					Engine.theApp.schemaManager.clearCache(getProjectTreeObject().getName());
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not rename folder from \""+oldPath+"\" to \""+newPath+"\" !");
				}
			}
			
			if (connector instanceof CouchDbConnector) {
				CouchDbManager.syncDocument(connector);
		    	try {
					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not reload connector \""+connector.getName()+"\" in tree !");
				}
			}
		}
	}
}
