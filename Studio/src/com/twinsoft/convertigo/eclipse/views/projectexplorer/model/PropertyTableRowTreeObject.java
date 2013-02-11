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

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class PropertyTableRowTreeObject extends TreeParent implements IPropertyTreeObject, IOrderableTreeObject, IPropertySource, IActionFilter {

	protected IPropertyDescriptor[] propertyDescriptors;
	
	protected PropertyTableRowTreeObject(Viewer viewer, XMLVector<Object> row) {
		super(viewer, row);
		propertyDescriptors = loadDescriptors();
		loadColumns();
	}

	@Override
	public XMLVector<Object> getObject(){
		return GenericUtils.cast(super.getObject());
	}
	
	public String getColumnDefaultLabel() {
		return "Column";
	}
	
	public boolean isInherited() {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			return table.isInherited();
		return false;
	}
	
	public IPropertyTreeObject addNewColumn() {
		return addColumn(null);
	}
	
	public IPropertyTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof PropertyData) {
			PropertyData propertyData = (PropertyData)object;
			Class<? extends TreeParent> c = propertyData.getOwnerClass();
			if (c.equals(getColumnClass())) {
				XMLVector<Object> col = propertyData.getData();
				if (bChangeName) col.set(0, getAvailableColumnName((String)col.get(0), 0));
				return addColumn(newColumn(col));
			}
			else if (c.equals(getClass())) {
				PropertyTableTreeObject table = getParentTable();
				if (table != null)
					return table.add(object, bChangeName);
			}
		} else if (object instanceof PropertyTableRowTreeObject) {
			PropertyTableTreeObject table = getParentTable();
			if (table != null)
				return table.add(object, false);
		} else if (object instanceof PropertyTableColumnTreeObject)
			return addColumn((PropertyTableColumnTreeObject)object);
		return null;
	}
	
	private String getAvailableColumnName(String colName, int index) {
		String name = colName+ (index==0?"":"-"+index);
		if (!isAvailableColumnName(name))
			return getAvailableColumnName(colName, index+1);
		return name;
	}

	private boolean isAvailableColumnName(String colName) {
		for (PropertyTableColumnTreeObject col: getChildren()) {
			if (col.getName().equals(colName))
				return false;
		}
		return true;
	}
	
	public void remove(Object object) {
		if (object.equals(this)) {
			PropertyTableTreeObject table = getParentTable();
			if (table != null) {
				table.remove(object);
			}
		}
		else if (object instanceof PropertyTableColumnTreeObject) {
			removeColumn((PropertyTableColumnTreeObject)object);
		}
	}

	public void removeColumn(PropertyTableColumnTreeObject tableCol) {
		try {
			if (!isInherited() && (tableCol != null)) {
				XMLVector<Object> row = getObject();
				XMLVector<XMLVector<Object>> cols = GenericUtils.cast(row.get(getColumsIndex()));
				cols.remove(indexOf(tableCol));
				removeChild(tableCol);
				hasBeenModified();
			}
		}
		catch (Exception e) {};
	}
	
	public PropertyTableColumnTreeObject moveColumn(PropertyTableColumnTreeObject tableCol, boolean up) {
		try {
			if (!isInherited() && (tableCol != null)) {
				int index = indexOf(tableCol);
				int pos = index +(up ? -1:2);
				if ((pos >= 0) && (pos <= numberOfChildren())) {
					XMLVector<Object> row = getObject();
					XMLVector<XMLVector<Object>> cols = GenericUtils.cast(row.get(getColumsIndex()));
					XMLVector<Object> col = tableCol.getObject();
					PropertyTableColumnTreeObject newCol = newColumn(new XMLVector<Object>(col));
					cols.add(pos, col);
					cols.remove(pos + (up ? 2:-2));
					addChild(pos,newCol);
					removeChild(tableCol);
					hasBeenModified();
					return newCol;
				}
			}
		}
		catch (Exception e) {};
		return null;
	}
	
	private void loadColumns() {
		try {
			XMLVector<Object> row = getObject();
			XMLVector<XMLVector<Object>> cols = GenericUtils.cast(row.get(getColumsIndex()));
			for(XMLVector<Object> col : cols) {
				addChild(newColumn(col));
			}
		}
		catch (Exception e) {};
	}
	
	private PropertyTableColumnTreeObject addColumn(PropertyTableColumnTreeObject col) {
		PropertyTableColumnTreeObject tableCol = null;
		try {
			tableCol = (col == null) ? newColumn():col;
			if (tableCol != null) {
				XMLVector<Object> row = getObject();
				XMLVector<XMLVector<Object>> cols = GenericUtils.cast(row.get(getColumsIndex()));
				cols.add(tableCol.getObject());
				addChild(tableCol);
				hasBeenModified();
			}
		}
		catch (Exception e) {};
		return tableCol;
	}
	
	protected boolean isBoolean(String propertyName) {
		return false;
	}
	
	protected IPropertyDescriptor[] loadDescriptors() {
		return new IPropertyDescriptor[] {};
	}
	
	protected Class<? extends PropertyTableColumnTreeObject> getColumnClass() {
		return PropertyTableColumnTreeObject.class;
	}

	protected int getColumsIndex() {
		return 1;
	}
	
	protected boolean canAddColums() {
		return true;
	}
	
	protected XMLVector<Object> createColumn(String name) {
		XMLVector<Object> col = new XMLVector<Object>();
		col.add(name);
		return col;
	}
	
	private PropertyTableColumnTreeObject newColumn() {
		if (!isInherited() && canAddColums()) {
			String label = getColumnDefaultLabel().toLowerCase();
			String name = label;
			int index = 0;
			do {
				index++;
				name = label+ index;
			} while (!isAvailableColumnName(name));
			return newColumn(createColumn(name));
		}
		return null;
	}
	
	protected PropertyTableColumnTreeObject newColumn(XMLVector<Object> col) {
		return new PropertyTableColumnTreeObject(viewer,col);
	}
	
	public PropertyTableTreeObject getParentTable() {
		if (parent != null) {
			return (PropertyTableTreeObject)getParent();
		}
		return null;
	}
	
	protected void hasBeenModified() {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			table.hasBeenModified();
	}
	
	@Override
	public String getName() {
		return toString();
	}

	@Override
	public String toString() {
		return (String) getObject().get(0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
	 */
	public Object getEditableValue() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#isPropertySet(java.lang.Object)
	 */
	public boolean isPropertySet(Object id) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
	 */
	public void resetPropertyValue(Object id) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
	 */
	public Object getPropertyValue(Object id) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object, java.lang.Object)
	 */
	public void setPropertyValue(Object id, Object value) {
		
		hasBeenModified();
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean testAttribute(Object target, String name, String value) {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			return table.testAttribute(target, name, value);
		return false;
	}
	
	@Override
	public List<? extends PropertyTableColumnTreeObject> getChildren(){
		return GenericUtils.<List<? extends PropertyTableColumnTreeObject>>cast(super.getChildren());
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#toXml(org.w3c.dom.Document)
	 */
	public Element toXml(Document document) {
		Element element = document.createElement("row");
		element.setAttribute("classname", getClass().getName());
		int len = propertyDescriptors.length;
		for (int i=0; i<len; i++) {
			String propertyName = (String)propertyDescriptors[i].getId();
			Element propertyElement = document.createElement("property");
			propertyElement.setAttribute("name", propertyName);
			try {
				Object value = getPropertyValue(propertyName);
				value = isBoolean(propertyName) ? ((((Integer) value).intValue() == 1)? Boolean.TRUE:Boolean.FALSE):value;
	            Node node = XMLUtils.writeObjectToXml(document, value);
	            propertyElement.appendChild(node);
			}
            catch (Exception e) {
            	ConvertigoPlugin.logException(e, "Skipping property \"" + propertyName + "\".");
            }
    		element.appendChild(propertyElement);
		}
		
		if (getColumnClass() != null) {
			Element propertyElement = document.createElement("property");
			propertyElement.setAttribute("name", "columns");
			element.appendChild(propertyElement);
		}
		
		for(PropertyTableColumnTreeObject colTreeObject : getChildren())
			element.appendChild(colTreeObject.toXml(document));
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
				
				NodeList childNodes = node.getChildNodes();
				int len = childNodes.getLength();
		            
				Node childNode, childValue;
				String childNodeName, propertyName;
				for (int i = 0 ; i < len ; i++) {
					childNode = childNodes.item(i);
					if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;
					childNodeName = childNode.getNodeName();
					if ((childNodeName.equalsIgnoreCase("property"))) {
						propertyName = ((Element)childNode).getAttribute("name");
						if (propertyName.equalsIgnoreCase("columns")) {
							xmlv.add(new XMLVector<Object>());
						}
						else {
							NodeList childValues = childNode.getChildNodes();
							for (int j=0; j<childValues.getLength(); j++) {
								childValue = childValues.item(j);
								if (childValue.getNodeType() != Node.ELEMENT_NODE) continue;
								Object value = XMLUtils.readObjectFromXml((Element)childValue);
								xmlv.add(value);
								break;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
            String message = "Unable to set the object properties from the serialized XML data.\n" +
			"Object class: '" + classname;
	        EngineException ee = new EngineException(message, e);
	        throw ee;
		}
		if (xmlv != null) {
			return new PropertyData(PropertyTableRowTreeObject.class, xmlv);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#getTreeObjectOwner()
	 */
	public TreeParent getTreeObjectOwner() {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			return table.getTreeObjectOwner();
		return null;
	}
}
