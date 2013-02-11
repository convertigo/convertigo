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
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class XMLTableDescriptionRowTreeObject extends PropertyTableRowTreeObject implements IXPathable {

	protected XMLTableDescriptionRowTreeObject(Viewer viewer, XMLVector<Object> row) {
		super(viewer, row);
	}

	@Override
	public XMLVector<Object> getObject(){
		return GenericUtils.cast(super.getObject());
	}
	
	public static Object read(Node node) throws EngineException {
		PropertyData pd = (PropertyData)PropertyTableRowTreeObject.read(node);
		if (pd != null) {
			return new PropertyData(XMLTableDescriptionRowTreeObject.class,pd.getData());
		}
		return null;
	}

	@Override
	protected Class<? extends PropertyTableColumnTreeObject> getColumnClass() {
		return XMLTableDescriptionColumnTreeObject.class;
	}

	@Override
	protected IPropertyDescriptor[] loadDescriptors() {
		IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[2];
		propertyDescriptors[0] = new TextPropertyDescriptor("tagname", "Row tag name");
		((PropertyDescriptor)propertyDescriptors[0]).setCategory("Configuration");
		propertyDescriptors[1] = new TextPropertyDescriptor("rowxpath", "Row XPath");
		((PropertyDescriptor)propertyDescriptors[1]).setCategory("Selection");
		return propertyDescriptors;
	}
	
	@Override
	protected int getColumsIndex() {
		return 2;
	}

	@Override
	protected XMLVector<Object> createColumn(String name) {
		XMLVector<Object> col = new XMLVector<Object>();
		col.add(name);
		col.add("./TD");
		col.add(Boolean.FALSE);
		return col;
	}

	@Override
	protected PropertyTableColumnTreeObject newColumn(XMLVector<Object> col) {
		return new XMLTableDescriptionColumnTreeObject(viewer,col);
	}

	@Override
	public String toString() {
		return (String)getObject().get(0);
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id == null) return null;
		String propertyName = (String) id;
		XMLVector<Object> row = getObject();
		if (propertyName.equals("tagname"))
			return row.get(0);
		if (propertyName.equals("rowxpath"))
			return row.get(1);
		return null;
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		if (id == null) return;
		if (value == null) return;
		if (isInherited()) return;
		
		String propertyName = (String) id;
		XMLVector<Object> row = getObject();
		if (propertyName.equals("tagname"))
			row.set(0, value);
		if (propertyName.equals("rowxpath"))
			row.set(1, value);
		
		super.setPropertyValue(id, value);
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.IXPathable#getXpath()
	 */
	public String getXpath() {
		return (String)getPropertyValue("rowxpath");
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.IXPathable#setXpath(java.lang.String)
	 */
	public void setXpath(String xpath) {
		setPropertyValue("rowxpath",xpath);
	}

}
