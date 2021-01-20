/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class MobileApplicationTreeObject extends DatabaseObjectTreeObject implements INamedSourceSelectorTreeObject {
	
	public MobileApplicationTreeObject(Viewer viewer, MobileApplication object) {
		super(viewer, object);
	}

	public MobileApplicationTreeObject(Viewer viewer, MobileApplication object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public MobileApplication getObject() {
		return (MobileApplication) super.getObject();
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
		refreshResourceFolder();
	}
	
	private void refreshResourceFolder() {
		try {
			ProjectTreeObject projectTreeObject = getProjectTreeObject();
			if (projectTreeObject != null) {
				projectTreeObject.getIProject().getFolder(getObject().getRelativeResourcePath()).refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		} catch (Exception e) {
			ConvertigoPlugin.logWarning(e, "Failed to refresh the mobile platform folder in resource view", false);
		}
	}

	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
				if (this.equals(doto)) {
					if (propertyName.equals("applicationName")) {
						if (!newValue.equals(oldValue)) {
							ApplicationComponent app = (ApplicationComponent)((MobileApplication)dbo).getApplicationComponent();
							app.markPwaAsDirty();
						}
					}
				}
			} catch (Exception e) {
				
			}
		}
	}

	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {
			@Override
			Object thisTreeObject() {
				return MobileApplicationTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (ProjectTreeObject.class.isAssignableFrom(c) ||
					ConnectorTreeObject.class.isAssignableFrom(c))
				{
					list.add("fsConnector");
				}
				
				if (ProjectTreeObject.class.isAssignableFrom(c) ||
					ConnectorTreeObject.class.isAssignableFrom(c) ||
					DesignDocumentTreeObject.class.isAssignableFrom(c))
				{
						list.add("fsDesignDocument");
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				return "fsConnector".equals(propertyName) || "fsDesignDocument".equals(propertyName);
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if ("fsConnector".equals(propertyName)) {
					return nsObject instanceof FullSyncConnector;
				}
				else if ("fsDesignDocument".equals(propertyName)) {
					if (nsObject instanceof DesignDocument) {
						DatabaseObject dboParent = ((DesignDocument)nsObject).getParent();
						if (dboParent instanceof FullSyncConnector) {
							return dboParent.getTokenPath(null).equals(getPropertyValue("fsConnector"));
						}
					}
				}
				return false;
			}
			
			@Override
			protected void handleSourceCleared(String propertyName) {
				if ("fsConnector".equals(propertyName)) {
					getObject().setFsDesignDocument("");
				}
			}
			
			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if ("fsConnector".equals(propertyName)) {
								getObject().setFsConnector(_pValue);
								hasBeenRenamed = true;
							}
							else if ("fsDesignDocument".equals(propertyName)) {
								getObject().setFsDesignDocument(_pValue);
								hasBeenRenamed = true;
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(MobileApplicationTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
}
