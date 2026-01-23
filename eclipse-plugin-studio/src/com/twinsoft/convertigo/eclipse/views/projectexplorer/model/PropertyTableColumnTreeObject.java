/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class PropertyTableColumnTreeObject extends TreeParent implements IPropertyTreeObject, IOrderableTreeObject, IPropertySource, IActionFilter {

	private IPropertyDescriptor[] propertyDescriptors = null;
	
	protected PropertyTableColumnTreeObject(Viewer viewer, XMLVector<Object> col) {
		super(viewer, col);
		propertyDescriptors = loadDescriptors();
	}

	@Override
	public XMLVector<Object> getObject(){
		return GenericUtils.<XMLVector<Object>>cast(super.getObject());
	}
	
	private IPropertyDescriptor[] loadDescriptors() {
		return new IPropertyDescriptor[]{};
	}
	
	private PropertyTableRowTreeObject getParentRow() {
		if (parent != null) {
			return (PropertyTableRowTreeObject)getParent();
		}
		return null;
	}

	public PropertyTableTreeObject getParentTable() {
		if (parent != null) {
			return getParentRow().getParentTable();
		}
		return null;
	}
	
	private void hasBeenModified() {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			table.hasBeenModified();
	}

	public boolean isInherited() {
		PropertyTableTreeObject table = getParentTable();
		if (table != null)
			return table.isInherited();
		return false;
	}

	public IPropertyTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof PropertyData) {
			PropertyData propertyData = (PropertyData)object;
			Class<? extends TreeParent> c = propertyData.getOwnerClass();
			if (c.equals(getClass())) {
				PropertyTableRowTreeObject row = getParentRow();
				if (row != null)
					return row.add(object, bChangeName);
			}
		}
		else if (object instanceof PropertyTableColumnTreeObject) {
			PropertyTableRowTreeObject row = getParentRow();
			if (row != null)
				return row.add(object, false);
		}
		return null;
	}
	
	public void remove(Object object) {
		if (object.equals(this)) {
			if (parent != null) {
				getParentRow().remove(object);
			}
		}
	}
	
	@Override
	public String getName() {
		return toString();
	}
	
	@Override
	public String toString() {
		return (String)getObject().get(0);
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
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#toXml(org.w3c.dom.Document)
	 */
	public Element toXml(Document document) {
		Element element = document.createElement("column");
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
		return element;
	}

	private boolean isBoolean(String propertyName) {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject#getTreeObjectOwner()
	 */
	public TreeParent getTreeObjectOwner() {
		PropertyTableRowTreeObject row = getParentRow();
		if (row != null) {
			PropertyTableTreeObject table = row.getParentTable();
			if (table != null)
				return table.getTreeObjectOwner();
		}
		return null;
	}
}
