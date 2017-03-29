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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;


public abstract class TreeObject implements IAdaptable {
	private Object object;
	public TreeParent parent;
	public final Viewer viewer;
	
	public TreeObject(Viewer viewer, Object object) {
		this.viewer = viewer;
		this.object = object;
	}

	public void update() {
		// does nothing
	}
	
	protected void remove() {
		// does nothing
	}
	
	public String getName() {
		return object.toString();
	}

	public void setParent(TreeParent parent) {
		this.parent = parent;
		if (parent == null)
			remove();
	}

	public TreeParent getParent() {
		return parent;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public String getPath() {
		String name, parentPath = "";
		
		name = getName();
		try {
			parentPath = parent.getPath();
		}
		catch (Exception e) {}
		
		return parentPath + ((parentPath.equals("")||name.equals("")) ? "":"/") + name;
	}
	
	public TreeParent[] getParents(boolean addThis) {
		TreeParent[] parents = null;
		try {
			int i, j, size;
			List<TreeObject> v = getParents();
			
			if (addThis)
				v.add(0, this);
			
			size = v.size();
			j = size-1;
			parents = new TreeParent[size];
			for (i=0;i<size;i++) {
				parents[i] = (TreeParent)v.get(j--);				
			}
		}
		catch (Exception e) {}
		
		return parents;
	}
	
	protected List<TreeObject> getParents() {
		List<TreeObject> v = new ArrayList<TreeObject>();
		TreeParent treeParent = parent;
		while (treeParent != null) {
			v.add(treeParent);
			treeParent = treeParent.getParent();
		}
		return v;
	}
	
	public boolean isChildOf(TreeObject treeObject) {
		if (treeObject != null) {
			return getParents().contains(treeObject);
		}
		return false;
	}
	
	public ProjectTreeObject getProjectTreeObject() {
		ProjectTreeObject projectTreeObject = null;
		
		if (this instanceof ProjectTreeObject)
			projectTreeObject = (ProjectTreeObject)this;
		else {
			TreeParent treeParent = parent;
			while (treeParent != null) {
				if (treeParent instanceof ProjectTreeObject) {
					projectTreeObject = (ProjectTreeObject)treeParent;
					break;
				}
				treeParent = treeParent.getParent();
			}
		}
		
		return projectTreeObject;
	}
	
	public ConnectorTreeObject getConnectorTreeObject() {
		ConnectorTreeObject connectorTreeObject = null;
		
		if (this instanceof ProjectTreeObject)
			return null;
			
		if (this instanceof ConnectorTreeObject)
			connectorTreeObject = (ConnectorTreeObject)this;
		else {
			TreeParent treeParent = parent;
			while (treeParent != null) {
				if (treeParent instanceof ConnectorTreeObject) {
					connectorTreeObject = (ConnectorTreeObject)treeParent;
					break;
				}
				treeParent = treeParent.getParent();
			}
		}
		
		return connectorTreeObject;
	}
	
	
	public String toString() {
		return object.toString();
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(object))
			return object;
		if (adapter.isInstance(viewer))
			return viewer;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	public TreeObject getNextSibling() {
		return getParent().getNextSibling(this);
	}
	
	public TreeObject getPreviousSibling() {
		return getParent().getPreviousSibling(this);
	}
	
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isProjectModified")) {
			Boolean bool = Boolean.valueOf(value);
			try {
				return bool.equals(Boolean.valueOf(getProjectTreeObject().hasChanged() || getProjectTreeObject().getModified()));
			} catch (Exception e) {}
		}
		return false;
	}
}
