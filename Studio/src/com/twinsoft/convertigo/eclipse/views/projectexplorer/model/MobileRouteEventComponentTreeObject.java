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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteFullsyncEvent;
import com.twinsoft.convertigo.beans.mobile.components.RouteSequenceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class MobileRouteEventComponentTreeObject extends MobileComponentTreeObject implements INamedSourceSelectorTreeObject {

	public MobileRouteEventComponentTreeObject(Viewer viewer, RouteEventComponent object) {
		super(viewer, object);
	}

	public MobileRouteEventComponentTreeObject(Viewer viewer, RouteEventComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public RouteEventComponent getObject() {
		return (RouteEventComponent) super.getObject();
	}

	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {

			@Override
			Object thisTreeObject() {
				return MobileRouteEventComponentTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof RouteEventComponent) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c))
					{
						list.add("source");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof RouteEventComponent) {
					return "source".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof RouteEventComponent) {
					if ("source".equals(propertyName)) {
						RouteEventComponent rec = getObject();
						if (rec instanceof RouteSequenceEvent) {
							return nsObject instanceof Sequence;
						}
						if (rec instanceof RouteFullsyncEvent) {
							return nsObject instanceof FullSyncConnector;
						}
					}
				}
				return false;
			}

			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}

			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof RouteEventComponent) {
								if ("source".equals(propertyName)) {
									((RouteEventComponent)getObject()).setSource(_pValue);
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
		
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
		if (bModified && !isInherited) {
			markRouteAsDirty();
		}
	}
	
	protected void markRouteAsDirty() {
		TreeParent treeParent = parent;
		while (treeParent != null) {
			if (treeParent instanceof MobileApplicationComponentTreeObject) {
				((MobileApplicationComponentTreeObject) treeParent).markRouteAsDirty();
				break;
			}
			treeParent = treeParent.getParent();
		}
	}

}
