/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.Engine;

public class ReferenceTreeObject extends DatabaseObjectTreeObject {

	public ReferenceTreeObject(Viewer viewer, Reference object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public Reference getObject() {
		return (Reference) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("canUpdate")) {
			ReferenceTreeObject refTreeObject = (ReferenceTreeObject) target;
			if (refTreeObject.getObject() instanceof ProjectSchemaReference ref) {
				return StringUtils.isNoneBlank(ref.getParser().getGitUrl());
			} else if (refTreeObject.getObject() instanceof RemoteFileReference remoteFile) {
				return !remoteFile.getFilepath().isEmpty() && !remoteFile.getUrlpath().isEmpty(); 
			} 
		} 
		return super.testAttribute(target, name, value);
	}

	private void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		int update = treeObjectEvent.update;
		
		// Updates project name references
		if (update != TreeObjectEvent.UPDATE_NONE) {
			boolean isLocalProject = false;
			boolean isSameValue = false;
			boolean shouldUpdate = false;
			
			if (getObject() instanceof ProjectSchemaReference) {
				ProjectSchemaReference reference = (ProjectSchemaReference)getObject();
				
				// Case of project rename
				if (databaseObject instanceof Project) {
					isLocalProject = reference.getProject().equals(databaseObject);
					isSameValue = reference.getParser().getProjectName().equals(oldValue);
					shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
					if (isSameValue && shouldUpdate) {
						reference.setProjectName((String)newValue);
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		}
	}
	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			String propertyName = treeObjectEvent.propertyName;
			propertyName = ((propertyName == null) ? "":propertyName);
			
			// If a bean name has changed
			if (propertyName.equals("name")) {
				handlesBeanNameChanged(treeObjectEvent);
			}
			else {
				// If a referenced project has changed, clear current project schema
				if (getObject() instanceof ProjectSchemaReference) {
					ProjectSchemaReference reference = (ProjectSchemaReference)getObject();
					if (databaseObject.getProject().getName().equals(reference.getParser().getProjectName())) {
						Engine.theApp.schemaManager.clearCache(reference.getProject().getName());
					}
				}
			}
		}
	}	
}
