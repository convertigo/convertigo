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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.ViewObject;

public class DesignDocumentViewTreeObject extends TreeParent implements IPropertySource {

	private IPropertyDescriptor[] propertyDescriptors;
	
	public DesignDocumentViewTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
		propertyDescriptors = loadDescriptors();
		loadFunctions();
	}

	@Override
	public ViewObject getObject() {
		return (ViewObject)super.getObject();
	}

	public TreeParent getTreeObjectOwner() {
		return getParent().getParent();
	}
	
	private IPropertyDescriptor[] loadDescriptors() {
		IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[1];
		propertyDescriptors[0] = new TextPropertyDescriptor("viewname", "View name");
		((PropertyDescriptor)propertyDescriptors[0]).setCategory("Configuration");
		return propertyDescriptors;
	}
	
	private void loadFunctions() {
		ViewObject view = getObject();
		if (view.hasMap())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getMap()));
		if (view.hasReduce())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getReduce()));
	}

	private DesignDocumentTreeObject getDesignDocumentTreeObject() {
		return (DesignDocumentTreeObject) getTreeObjectOwner();
	}
	
	protected void hasBeenModified() {
		DesignDocumentTreeObject ddto = getDesignDocumentTreeObject();
		if (ddto != null) {
			ddto.hasBeenModified();
		}
	}
	
	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	@Override
	public boolean isPropertySet(Object arg0) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object arg0) {
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id == null) return null;
		String propertyName = (String) id;
		if (propertyName.equals("viewname"))
			return getObject().getName();
		return null;
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		if (id == null) return;
		if (value == null) return;
		if (!isValidPropertyValue(id, value)) return;
		
		String propertyName = (String) id;
		if (propertyName.equals("viewname")) {
			getObject().setName(value.toString());
		}
		
		hasBeenModified();
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
	}

	private boolean isValidPropertyValue(Object id, Object value) {
		DesignDocumentTreeObject ddto = getDesignDocumentTreeObject();
		if (ddto == null) return false;
		if (id == null) return false;
		if (value == null) return false;
		
		String propertyName = (String) id;
		if (propertyName.equals("viewname")) {
			return !ddto.hasView(value.toString());
		}
		return true;
	}
}
