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

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class UrlMappingOperationTreeObject extends DatabaseObjectTreeObject implements INamedSourceSelectorTreeObject {

	public UrlMappingOperationTreeObject(Viewer viewer, DatabaseObject object) {
		this(viewer, object, false);
	}

	public UrlMappingOperationTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public UrlMappingOperation getObject() {
		return (UrlMappingOperation) super.getObject();
	}

	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {

			@Override
			Object thisTreeObject() {
				return UrlMappingOperationTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof UrlMappingOperation) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c) ||
						TransactionTreeObject.class.isAssignableFrom(c))
					{
						list.add("targetRequestable");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof UrlMappingOperation) {
					return "targetRequestable".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof UrlMappingOperation) {
					if ("targetRequestable".equals(propertyName)) {
						return nsObject instanceof RequestableObject;
					}
				}
				return false;
			}

			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}

			@Override
			protected void handleSourceRenamed(int update, String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					boolean isLocal = oldName.startsWith(getProjectTreeObject().getName());
					boolean shoudRename = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && isLocal);
					
					String pValue = (String) getPropertyValue(propertyName);
					if (shoudRename && pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof UrlMappingOperation) {
								if ("targetRequestable".equals(propertyName)) {
									((UrlMappingOperation)getObject()).setTargetRequestable(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
	
}
