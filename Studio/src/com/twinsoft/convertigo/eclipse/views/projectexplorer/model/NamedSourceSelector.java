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

import com.twinsoft.convertigo.beans.core.ITokenPath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public abstract class NamedSourceSelector {

	abstract Object thisTreeObject();
	
	protected void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		int update = treeObjectEvent.update;
				
		if (thisTreeObject().equals(treeObject) && isNamedSource(propertyName)) {
			if ("".equals(newValue) && !newValue.equals(oldValue)) {
				handleSourceCleared(propertyName);
			}
		}
		else if (update != TreeObjectEvent.UPDATE_NONE && propertyName.equals("name")) {
			if (!newValue.equals(oldValue)) {
				String oldTokenPath = null, newTokenPath = null;
				if (treeObject instanceof DatabaseObjectTreeObject) {
					if (treeObject.getObject() instanceof ITokenPath) {
						oldTokenPath = ((ITokenPath)treeObject.getObject()).getTokenPath((String)oldValue);
						newTokenPath = ((ITokenPath)treeObject.getObject()).getTokenPath((String)newValue);
					}
				}
				else if (treeObject instanceof IDesignTreeObject) {
					TreeObject nsTreeObject = (TreeObject) ((IDesignTreeObject)treeObject).getParentDesignTreeObject();
					if (nsTreeObject.getObject() instanceof ITokenPath) {
						oldTokenPath = ((ITokenPath)nsTreeObject.getObject()).getTokenPath(null) +"."+ (String)oldValue;
						newTokenPath = ((ITokenPath)nsTreeObject.getObject()).getTokenPath(null) +"."+ (String)newValue;
					}
				}

				if (oldTokenPath != null && newTokenPath != null) {
					boolean shoudRename = (update == TreeObjectEvent.UPDATE_ALL) 
							|| ((update == TreeObjectEvent.UPDATE_LOCAL) && fromSameProject(treeObject));
					
					if (shoudRename) {
						for (String _propertyName : getPropertyNamesForSource(treeObject.getClass())) {
							handleSourceRenamed(_propertyName, oldTokenPath, newTokenPath);
						}
					}
				}
			}
		}
	}
	
	private boolean fromSameProject(TreeObject treeObject) {
		try {
			Object ob = thisTreeObject();
			if (ob instanceof DatabaseObjectTreeObject && treeObject instanceof DatabaseObjectTreeObject) {
				return ((DatabaseObjectTreeObject)ob).getProjectTreeObject().equals(((DatabaseObjectTreeObject)treeObject).getProjectTreeObject());
			}
		}
		catch (Throwable t) {}
		return false;
	}
		
	protected List<String> getPropertyNamesForSource(Class<?> c) {
		return new ArrayList<String>();
	}
	
	protected boolean isNamedSource(String propertyName) {
		return false;
	}
	
	public boolean isSelectable(String propertyName, Object nsObject) {
		return false;
	}
	
	protected void handleSourceCleared(String propertyName) {
		
	}
	
	protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
		
	}
}
