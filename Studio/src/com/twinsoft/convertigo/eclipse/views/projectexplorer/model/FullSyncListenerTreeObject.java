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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/DesignDocumentTreeObject.java $
 * $Author: nicolasa $
 * $Revision: 39218 $
 * $Date: 2015-02-23 12:21:31 +0100 (lun., 23 févr. 2015) $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.StringTokenizer;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.FullSyncListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class FullSyncListenerTreeObject extends ListenerTreeObject {

	public FullSyncListenerTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public FullSyncListener getObject() {
		return (FullSyncListener) super.getObject();
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		String propertyName = treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "":propertyName);
		
		if (propertyName.equals("name")) {
			handlesBeanNameChanged(treeObjectEvent);
		}
	}

	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		int update = treeObjectEvent.update;

		if (update != TreeObjectEvent.UPDATE_NONE) {
			String _targetSequence = null;
			String targetSequence = getObject().getTargetSequence();
			if (targetSequence != null) {
				StringTokenizer st = new StringTokenizer(targetSequence,".");
				String projectName = st.nextToken();
				String sequenceName = st.nextToken();
				
				if (treeObject instanceof DatabaseObjectTreeObject) {
					DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
					if (databaseObject instanceof Project && oldValue.equals(projectName)) {
						_targetSequence = newTargetSequence(newValue.toString(),sequenceName);
					}
					else if (databaseObject instanceof Sequence && oldValue.equals(sequenceName)) {
						if (projectName.equals(databaseObject.getProject().getName())) {
							_targetSequence = newTargetSequence(projectName, newValue.toString());
						}
					}
				}
			}
			
			String _targetView = null;
			String targetView = getObject().getTargetView();
			if (targetView != null) {
				StringTokenizer st = new StringTokenizer(targetView,".");
				String projectName = st.nextToken();
				String connectorName = st.nextToken();
				String documentName = st.nextToken();
				String viewName = st.nextToken();
				
				if (treeObject instanceof DatabaseObjectTreeObject) {
					DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
					if (databaseObject instanceof Project && oldValue.equals(projectName)) {
						_targetView = newTargetView(newValue.toString(), connectorName, documentName, viewName);
					}
					else if (databaseObject instanceof FullSyncConnector && oldValue.equals(connectorName)) {
						if (projectName.equals(databaseObject.getProject().getName())) {
							_targetView = newTargetView(projectName, newValue.toString(), documentName, viewName);
						}
					}
					else if (databaseObject instanceof DesignDocument && oldValue.equals(documentName)) {
						if (projectName.equals(databaseObject.getProject().getName())) {
							if (connectorName.equals(databaseObject.getConnector().getName())) {
								_targetView = newTargetView(projectName, connectorName, newValue.toString(), viewName);
							}
						}
					}
				}
				else if (treeObject instanceof DesignDocumentViewTreeObject && oldValue.equals(viewName)) {
					DesignDocument designDocument = (DesignDocument) ((DesignDocumentViewTreeObject)treeObject).getTreeObjectOwner().getObject();
					if (projectName.equals(designDocument.getProject().getName())) {
						if (connectorName.equals(designDocument.getConnector().getName())) {
							if (documentName.equals(designDocument.getName())) {
								_targetView = newTargetView(projectName, connectorName, documentName, newValue.toString());
							}
						}
					}
				}
			}
			
			boolean shouldUpdate = false;
			if (_targetSequence != null) {
				getObject().setTargetSequence(_targetSequence);
				shouldUpdate = true;
			}
			if (_targetView != null) {
				getObject().setTargetView(_targetView);
				shouldUpdate = true;
			}
			
			if (shouldUpdate) {
				hasBeenModified(true);
				viewer.refresh();
				
				getDescriptors();// refresh editors (e.g labels in combobox)
			}
		}
	}
	
	private String newTargetSequence(String projectName, String sequenceName) {
		return projectName + "." + sequenceName;
	}
	
	private String newTargetView(String projectName, String connectorName, String documentName, String viewName) {
		return projectName + "." + connectorName + "." + documentName + "." + viewName;
	}
}
