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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public abstract class TreeParent extends TreeObject {
	private List<TreeObject> children;
	
	public TreeParent(Viewer viewer, Object object) {
		super(viewer, object);
		children = new ArrayList<TreeObject>();
	}
	
	public void addChild(TreeObject child) {
		synchronized (children) {
			children.add(child);			
		}
		child.setParent(this);
	}

	public void addChild(int index, TreeObject child) {
		synchronized (children) {
			children.add(index, child);
		}
		child.setParent(this);
	}
	
	public void removeChild(TreeObject child) {
		synchronized (children) {
			children.remove(child);
		}
		child.setParent(null);
		if (child instanceof TreeParent) {
			((TreeParent) child).removeAllChildren();
		}
	}
	
	public void removeAllChildren() {
		if (!children.isEmpty()) {
	    	for (TreeObject child : GenericUtils.clone(getChildren())) {
	    		if (child instanceof TreeParent) {
	    			((TreeParent) child).removeAllChildren();
	    		}
	    		removeChild(child);
	    	}
	    }
	}
	
	public TreeObject getPreviousSibling(TreeObject child) {
		TreeObject prevChild = null;
		synchronized (children) {
			if (!children.isEmpty()) {
				if (children.contains(child)) {
					int pos = children.indexOf(child);
					try {
						prevChild = (TreeObject) children.get(pos - 1);
					}
					catch (Exception e) {}
				}
			}
		}
		return prevChild;
	}
	public TreeObject getNextSibling(TreeObject child) {
		TreeObject nextChild = null;
		synchronized (children) {
			if (!children.isEmpty()) {
				if (children.contains(child)) {
					int pos = children.indexOf(child);
					try {
						nextChild = (TreeObject) children.get(pos + 1);
					}
					catch (Exception e) {}
				}
			}
		}
		return nextChild;
	}
	
	public List<? extends TreeObject> getChildren() {
		synchronized (children) {
			return GenericUtils.clone(children);
		}
	}
	
	public List<? extends TreeObject> getAllChildren() {
		List<TreeObject> list = new ArrayList<TreeObject>(); 
		if (hasChildren()) {
	    	for(TreeObject child : getChildren()) {
	    		list.add(child);
	    		if (child instanceof TreeParent) {
	    			list.addAll(((TreeParent) child).getAllChildren());
	    		}
	    	}
	    }
		return list;
	}
	
	public TreeObject findTreeObjectByUserObject(Object databaseObject) {
		TreeObject treeObject = null;
		if (databaseObject != null) {
			if (getObject().equals(databaseObject)) {
				treeObject = this;
			} else {
				for (TreeObject child : getChildren()) {
					if (child instanceof TreeParent) {
						treeObject = ((TreeParent) child).findTreeObjectByUserObject(databaseObject);
						if (treeObject != null) {
							break;
						}
					}
				}
			}
		}
		return treeObject;
	}
	
	public boolean hasChildren() {
		synchronized (children) {
			return !children.isEmpty();
		}
	}
	
	public int indexOf(TreeObject child) {
		synchronized (children) {
			return children.indexOf(child);
		}
	}
	
	public int numberOfChildren() {
		synchronized (children) {
			return children.size();
		}
	}
}
