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

public class TreePath {
	
	private Object[] components = new Object[]{};
	private String fullPath = "";
	private String delim = "/";
	private int count = 0;
	
	//Primarily provided for subclasses that represent paths in a different manner.
	protected  TreePath() {
	}
	
    //Constructs a TreePath containing only a single element. 
	public TreePath(Object singlePath) {
		String name = singlePath.toString();
		if (singlePath != null) {
			components = new Object[1];
			components[0] = singlePath;
			if (!name.equals(""))
				fullPath += delim + name;
			count++;
		}
	}
	
    //Constructs a path from an array of Objects, uniquely identifying the path from the root of the tree to a specific node, as returned by the tree's data model. 
	public TreePath(Object[] paths) {
		this(paths, paths.length);
	}
	
    ///Constructs a new TreePath with the identified path components of length length. 
	protected TreePath(Object[] paths, int length) {
		if (paths != null) {
			int len = paths.length;
			components = new Object[length];
			arrayCopy(paths, 0, components, len, true);
			count = len;
		}
	}
	
	//Constructs a new TreePath, which is the path identified by parent ending in lastElement.
	protected  TreePath(TreePath parent, Object lastElement) {
		this(parent.getPath(), parent.getPathCount()+1);
		components[count++] = lastElement;
	}
	
	
	//Tests two TreePaths for equality by checking each element of the paths for equality.
	public boolean equals(Object o) {
		if (o instanceof TreePath) {
			TreePath treePath = (TreePath)o; 
			if (treePath.getPathCount() == count) {
				for (int i=0;i<count;i++) {
					if (!(treePath.getPathComponent(i).equals(components[i])))
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	//Returns the last component of this path. 
	public Object getLastPathComponent() {
		Object object = null;
		if (count>0)
			object = components[count-1];
		return object;
	}
    
	//Returns a path containing all the elements of this object, except the last path component.
	public TreePath getParentPath() {
		TreePath parent = null;
		if (count>0) {
			Object[] objects = new Object[count-1];
			arrayCopy(components, 0, objects, count-1, false);
			parent = new TreePath(objects);
		}
		return parent;
	}
    
	//Returns an ordered array of Objects containing the components of this TreePath.
	public Object[] getPath() {
		Object[] objects = new Object[count];
		arrayCopy(components, 0, objects, count, false);
		return objects;
	}
    
	//Returns the path component at the specified index.
	public Object getPathComponent(int element) {
		Object object = null;
		if ((element>=0) && (element<count))
			object = components[element];
		return object;
	}
    
	//Returns the number of elements in the path.
	public int getPathCount() {
		return count;
	}
    
	//Returns the hashCode for the object.
	public int hashCode() {
		int hashcode = 0;
		//TODO: implements
		return hashcode;
	}
    
	//Returns true if aTreePath is a descendant of this TreePath.
	public boolean isDescendant(TreePath aTreePath) {
		return (fullPath.indexOf(aTreePath.toString()) > 0);
	}
    
	//Returns a new path containing all the elements of this object plus child.
	public TreePath pathByAddingChild(Object child) {
		return new TreePath(this, child);
	}
    
	public String toString() {
		return fullPath;
	}
	
	private void arrayCopy(Object[] source, int index, Object[] destination, int length, boolean bAdd) {
		if (source != null) {
			if (length <= destination.length) {
				if ((source.length >= index+length) && (index >= 0)) {
					String name = "";
					int j = index;
					for (int i=0; i<length; i++) {
						Object ob = source[j++];
						destination[i] = ob;
						name = ob.toString();
						if (bAdd) {
							if (!name.equals(""))
								fullPath += delim + name;
						}
					}
				}
			}
		}
	}
}
