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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncFilterListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncViewListener;

public class FullSyncListenerTreeObject extends ListenerTreeObject implements INamedSourceSelectorTreeObject {

	public FullSyncListenerTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public AbstractFullSyncListener getObject() {
		return (AbstractFullSyncListener) super.getObject();
	}
	
	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {
			@Override
			Object thisTreeObject() {
				return FullSyncListenerTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (ProjectTreeObject.class.isAssignableFrom(c) ||
					SequenceTreeObject.class.isAssignableFrom(c))
				{
					list.add("targetSequence");
				}
				
				if (ProjectTreeObject.class.isAssignableFrom(c) ||
					ConnectorTreeObject.class.isAssignableFrom(c) ||
					DesignDocumentTreeObject.class.isAssignableFrom(c) ||
					DesignDocumentViewTreeObject.class.isAssignableFrom(c))
				{
						list.add("targetView");
				}
				
				if (ProjectTreeObject.class.isAssignableFrom(c) ||
					ConnectorTreeObject.class.isAssignableFrom(c) ||
					DesignDocumentTreeObject.class.isAssignableFrom(c) ||
					DesignDocumentFilterTreeObject.class.isAssignableFrom(c))
				{
						list.add("targetFilter");
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				return "targetSequence".equals(propertyName) || "targetView".equals(propertyName);
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if ("targetSequence".equals(propertyName)) {
					return nsObject instanceof Sequence;
				}
				else if ("targetView".equals(propertyName) || "targetFilter".equals(propertyName)) {
					if (nsObject instanceof String) {
						return ((String) nsObject).startsWith(getObject().getParent().getTokenPath(null));
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
							if ("targetSequence".equals(propertyName)) {
								getObject().setTargetSequence(_pValue);
								hasBeenRenamed = true;
							}
							else if ("targetView".equals(propertyName)) {
								((AbstractFullSyncViewListener) getObject()).setTargetView(_pValue);
								hasBeenRenamed = true;
							}
							else if ("targetFilter".equals(propertyName)) {
								((AbstractFullSyncFilterListener) getObject()).setTargetFilter(_pValue);
								hasBeenRenamed = true;
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
