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

package com.twinsoft.convertigo.beans.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;

public abstract class Variable extends DatabaseObject implements IMultiValued, INillableProperty {

	private static final long serialVersionUID = -8671967475887212428L;
	
	public static final String DATA_DIRECTORY = "vb";
	
	protected Object value;
	private int visibility = 0;
	private String description = "new variable";
	
	transient private Set<String> nullProps = new HashSet<String>();
	
	public Variable() {
		super();
		databaseType = "Variable";
		setValueOrNull(null);
		
		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
		this.newPriority = priority;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Variable clonedObject = (Variable)super.clone();
		clonedObject.newPriority = newPriority;
		clonedObject.nullProps = nullProps;
		return clonedObject;
	}

	public Boolean isMultiValued() {
		return Boolean.FALSE;
	}
	
	protected Object getNewValue() {
		if (isMultiValued())
			return new XMLVector<Object>();
		else
			return "";
	}
	
	@SuppressWarnings("unchecked")
	protected Object getNewValue(Object value) {
		if (value == null)
			return getNewValue();
		
		if (isMultiValued()) {
			if (value instanceof XMLVector)
				return new XMLVector<Object>((XMLVector<Object>)value);
			else {
				XMLVector<Object> xmlv = new XMLVector<Object>();
				if (value instanceof Collection) {
					for (Object ob: (Collection<Object>)value) xmlv.add(ob);
				}
				else if (value.getClass().isArray()) {
					for (Object item: (Object[])value) xmlv.add(item);
				}
				else {
					if (!value.equals("")) xmlv.add(value);
				}
				return xmlv;
			}
		}
		else
			return value.toString();
	}
	
	public Object getValueOrNull() {
		if (!isNullProperty("value"))
			return getDefaultValue();
		return null;
	}
	
	public void setValueOrNull(Object value) {
		setNullProperty("value", (value==null)? Boolean.TRUE:Boolean.FALSE);
		setDefaultValue(getNewValue(value));
	}
	
	public Object getDefaultValue() {
		return value;
	}

	public void setDefaultValue(Object value) {
		this.value = value;
	}
	
	public int getVisibility() {
		return this.visibility;
	}
	
	public void setVisibility(int visibility) {
		this.visibility = visibility;
	}
	
	/*----- Keep for compatibility ----*/
	/**
	 * @deprecated since 5.0.3 beans version, replaced by isMultiValued()
	 */
	@Deprecated
	public Boolean isMulti() {
		//return multi;
		return isMultiValued();
	}

	/**
	 * @deprecated since 5.0.3 beans version, does nothing anymore
	 */
	@Deprecated
	public void setMulti(Boolean multi) {
		//this.multi = multi;
	}
	/*---------------------------------*/
	
	public String getPath() {
        return parent.getPath() + "/" + DATA_DIRECTORY ;
    }

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/*----- Keep for compatibility ----*/
	/**
	 * @deprecated since 5.0.3 beans version, replaced by getValueOrNull() which may return null
	 */
	@Deprecated
	public Object getValue() {
		//return value;
		return getDefaultValue();
	}

	/**
	 * @deprecated since 5.0.3 beans version, does nothing anymore
	 */
	public void setValue(Object value) {
		//this.value = value;
	}
	/*---------------------------------*/
	
	public Boolean isNullProperty(String propertyName) {
		return nullProps.contains(propertyName);
	}

	public void setNullProperty(String propertyName, Boolean isNull) {
		if (isNull)
			nullProps.add(propertyName);
		else
			nullProps.remove(propertyName);
	}
	
	/* (non-Javadoc)
	* @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	*/
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		try {
			newPriority = new Long(element.getAttribute("newPriority")).longValue();
			if (newPriority != priority) newPriority = priority;
		}
		catch(Exception e) {
			throw new Exception("Missing \"newPriority\" attribute");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the object "newPriority" value
        element.setAttribute("newPriority", new Long(newPriority).toString());
		
		return element;
	}
	
	protected String getLabel() throws EngineException {
		Object value = getValueOrNull();
		if (value!=null) {
			boolean isString = value instanceof String;
			return " ="+ (isString? "\"":"") + Visibility.Studio.printValue(visibility,value) + (isString? "\"":"");
		}
		return "";
	}
	
	@Override
	public boolean isCipheredProperty(String propertyName) {
		if (propertyName.equals("value"))
			return Visibility.XmlFile.isMasked(getVisibility());
		return super.isCipheredProperty(propertyName);
	}
	
	@Override
	public boolean isTraceableProperty(String propertyName) {
		if (propertyName.equals("value"))
			return !Visibility.Logs.isMasked(getVisibility());
		return super.isTraceableProperty(propertyName);
	}

	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if (propertyName.equals("value"))
			return target.isMasked(getVisibility());
		return false;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label = getLabel();
		} catch (EngineException e) {}
		return super.toString() + label;
	}	
}
