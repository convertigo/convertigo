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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class XMLRecordDescriptionRowTreeObject extends PropertyTableRowTreeObject implements IXPathable {

	protected XMLRecordDescriptionRowTreeObject(Viewer viewer, XMLVector<Object> row) {
		super(viewer, row);
	}

	@Override
	public XMLVector<Object> getObject(){
		return GenericUtils.cast(super.getObject());
	}
	
	public static Object read(Node node) throws EngineException {
		PropertyData pd = (PropertyData)PropertyTableRowTreeObject.read(node);
		if (pd != null)
			return new PropertyData(XMLRecordDescriptionRowTreeObject.class,pd.getData());
		return null;
	}

	@Override
	protected Class<? extends PropertyTableColumnTreeObject> getColumnClass() {
		return null;
	}

	@Override
	protected IPropertyDescriptor[] loadDescriptors() {
		IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[3];
		propertyDescriptors[0] = new TextPropertyDescriptor("name", "Data tag name");
		((PropertyDescriptor)propertyDescriptors[0]).setCategory("Configuration");
		propertyDescriptors[1] = new TextPropertyDescriptor("xpath", "Data XPath");
		((PropertyDescriptor)propertyDescriptors[1]).setCategory("Selection");
		propertyDescriptors[2] = new ComboBoxPropertyDescriptor("extract", "Extract children", new String[]{"false","true"});
		((PropertyDescriptor)propertyDescriptors[2]).setCategory("Selection");
		return propertyDescriptors;
	}

	@Override
	protected int getColumsIndex() {
		return -1; // means does not support columns
	}

	
	@Override
	protected boolean canAddColums() {
		return false;
	}

	@Override
	public String toString() {
		return (String)getObject().get(0);
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableRowTreeObject#isBoolean(java.lang.String)
	 */
	@Override
	protected boolean isBoolean(String propertyName) {
		if ("extract".equals(propertyName)) return true;
		return false;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id == null) return null;
		String propertyName = (String) id;
		XMLVector<Object> row = getObject();
		if (propertyName.equals("name"))
			return row.get(0);
		if (propertyName.equals("xpath"))
			return row.get(1);
		if (propertyName.equals("extract"))
			return (row.get(2).equals(Boolean.TRUE) ? 1:0);
		return null;
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		if (id == null) return;
		if (value == null) return;
		if (isInherited()) return;
		String propertyName = (String) id;
		XMLVector<Object> row = getObject();
		if (propertyName.equals("name"))
			row.set(0, value);
		if (propertyName.equals("xpath"))
			row.set(1, value);
		if (propertyName.equals("extract"))
			row.set(2, value.equals(1) ? Boolean.TRUE:Boolean.FALSE);
		super.setPropertyValue(id, value);
	}

	@Override
	public String getColumnDefaultLabel() {
		return "Item";
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.IXPathable#getXpath()
	 */
	public String getXpath() {
		return (String)getPropertyValue("xpath");
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.IXPathable#setXpath(java.lang.String)
	 */
	public void setXpath(String xpath) {
		setPropertyValue("xpath",xpath);
	}
}
