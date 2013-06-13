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

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class PropertyTableTreeObject extends TreeParent implements IPropertyTreeObject, IActionFilter {

	protected DatabaseObjectTreeObject databaseObjectTreeObject = null;
	protected XMLVector<XMLVector<Object>> data = null;
	
	/**
	 * @param viewer
	 * @param propertyName
	 * @param data
	 * @param databaseObjectTreeObject
	 */
	public PropertyTableTreeObject(Viewer viewer, String propertyName, XMLVector<XMLVector<Object>> data, DatabaseObjectTreeObject databaseObjectTreeObject) {
		super(viewer, propertyName);
		this.data = data;
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		loadRows();
	}
	
	@Override
	public String getObject(){
		return (String) super.getObject();
	}
	
	public String getRowDefaultLabel() {
		return "Row";
	}
	
	public IPropertyTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof PropertyData) {
			PropertyData propertyData = (PropertyData)object;
			Class<? extends TreeParent> c = propertyData.getOwnerClass();
			if (c.equals(getRowClass())) {
				XMLVector<Object> row = propertyData.getData();
				if (bChangeName) row.set(0, getAvailableRowName((String)row.get(0),0));
				return addRow(newRow(row));
			}
			else if (c.equals(getClass())) {
				reloadRows();
				return this;
			}
		}
		else if (object instanceof PropertyTableRowTreeObject) {
			return addRow((PropertyTableRowTreeObject)object);
		}
		return null;
	}

	private String getAvailableRowName(String rowName, int index) {
		String name = rowName+ (index==0?"":"-"+index);
		if (!isAvailableRowName(name))
			return getAvailableRowName(rowName, index+1);
		return name;
	}
	
	private boolean isAvailableRowName(String rowName) {
		for (PropertyTableRowTreeObject row: getChildren()) {
			if (row.getName().equals(rowName))
				return false;
		}
		return true;
	}

	public IPropertyTreeObject addNewRow() {
		return addRow(null);
	}
	
	public void remove(Object object) {
		if (object.equals(this)) {
			removeRows();
		}
		else if (object instanceof PropertyTableRowTreeObject) {
			removeRow((PropertyTableRowTreeObject)object);
		}
	}

	public void removeRow(PropertyTableRowTreeObject tableRow) {
		if (!isInherited() && (tableRow != null)) {
			removeChild(tableRow);
			hasBeenModified();
		}
	}
	
	public PropertyTableRowTreeObject moveRow(PropertyTableRowTreeObject tableRow, boolean up) {
		if (!isInherited() && tableRow != null) {
			int index = indexOf(tableRow);
			int pos = index + (up ? -1:2);
			if ((pos >= 0) && (pos <= numberOfChildren())) {
				XMLVector<Object> row = tableRow.getObject();
				PropertyTableRowTreeObject newRow = newRow(new XMLVector<Object>(row));
				addChild(pos, newRow);
				removeChild(tableRow);
				hasBeenModified();
				return newRow;
			}
		}
		return null;
	}
	
	public boolean isInherited() {
		return databaseObjectTreeObject.isInherited;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isInherited")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isInherited()));
		}
		if (name.equals("canPaste")) {
			boolean canPaste = ((ConvertigoPlugin.clipboardManagerSystem.isCopy) || (ConvertigoPlugin.clipboardManagerSystem.isCut));
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(canPaste));
		}
		if (name.equals("acceptColumns")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.TRUE);
		}
		return super.testAttribute(target, name, value);
	}

	private void loadRows() {
		if (data != null)
			for(XMLVector<Object> row : data)
				addChild(newRow(row));
	}
	
	private void removeRows() {
		for(PropertyTableRowTreeObject row : GenericUtils.clone(getChildren()))
			removeRow(row);
	}
	
	@Override
	public List<? extends PropertyTableRowTreeObject> getChildren(){
		return GenericUtils.<List<? extends PropertyTableRowTreeObject>>cast(super.getChildren());
	}
	
	private void reloadRows() {
		removeRows();
		loadRows();
	}
	
	protected Class<? extends PropertyTableRowTreeObject> getRowClass() {
		return PropertyTableRowTreeObject.class;
	}
	
	private PropertyTableRowTreeObject addRow(PropertyTableRowTreeObject row) {
		PropertyTableRowTreeObject tableRow = (row == null) ? newRow():row;
		if (tableRow != null) {
			addChild(tableRow);
			hasBeenModified();
		}
		return tableRow;
	}
	
	protected boolean canAddRows() {
		return true;
	}
	
	protected XMLVector<Object> createRow(String name) {
		XMLVector<Object> row = new XMLVector<Object>();
		row.add(name);
		row.add(new XMLVector<Object>());
		return row;
	}
	
	private PropertyTableRowTreeObject newRow() {
		if (!isInherited() && canAddRows()) {
			String label = getRowDefaultLabel().toLowerCase();
			String name = label;
			int index = 0;
			do {
				index++;
				name = label+ index;
			} while (!isAvailableRowName(name));
			return newRow(createRow(name));
		}
		return null;
	}
	
	protected PropertyTableRowTreeObject newRow(XMLVector<Object> row) {
		return new PropertyTableRowTreeObject(viewer,row);
	}
	
	protected synchronized void hasBeenModified() {
		if (isInherited()) return;
		
		XMLVector<XMLVector<Object>> xmlv = new XMLVector<XMLVector<Object>>();
		for(PropertyTableRowTreeObject rowTreeObject : getChildren()){
			XMLVector<Object> row = new XMLVector<Object>(rowTreeObject.getObject());
			xmlv.add(row);
		}
		data = xmlv;
		
		try {
			DatabaseObject databaseObject = databaseObjectTreeObject.getObject();
			java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = databaseObjectTreeObject.getPropertyDescriptor(getObject());
	        Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
	        Object args[] = { data };
	        setter.invoke(databaseObject, args);
	        databaseObject.hasChanged = true;
	        databaseObjectTreeObject.hasBeenModified(true);
		}
		catch (Exception e) {}
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(databaseObjectTreeObject, null);
		
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#toXml(org.w3c.dom.Document)
	 */
	public Element toXml(Document document) {
		Element element = document.createElement("table");
		element.setAttribute("classname", getClass().getName());
		element.setAttribute("property", getObject());
		for(PropertyTableRowTreeObject rowTreeObject : getChildren())
			element.appendChild(rowTreeObject.toXml(document));
		return element;
	}

	public static Object read(Node node) throws EngineException {
		String classname = null;
		XMLVector<Object> xmlv = null;
		try {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				classname = element.getAttribute("classname");
				xmlv = new XMLVector<Object>();
			}
		}
		catch (Exception e) {
            String message = "Unable to set the object properties from the serialized XML data.\n" +
			"Object class: '" + classname;
	        EngineException ee = new EngineException(message, e);
	        throw ee;
		}
		if (xmlv != null)
			return new PropertyData(PropertyTableTreeObject.class, xmlv);
		return null;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#getTreeObjectOwner()
	 */
	public TreeParent getTreeObjectOwner() {
		return getParent();
	}

}
