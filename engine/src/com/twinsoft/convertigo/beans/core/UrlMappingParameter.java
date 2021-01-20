/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.EnumUtils;

@DboCategoryInfo(
		getCategoryId = "UrlMappingParameter",
		getCategoryName = "Parameter",
		getIconClassCSS = "convertigo-action-newUrlMappingParameter"
	)
public abstract class UrlMappingParameter extends DatabaseObject implements ITagsProperty, INillableProperty{

	private static final long serialVersionUID = -2280875929012349646L;

	public enum Type {
		Path,
		Query,
		Body,
		Form,
		Header;
		
		public List<String> getAllowedDataTypes() {
			List<String> types = new ArrayList<String>();
			types.addAll(Arrays.asList(EnumUtils.toNames(DataType.class)));
			if (this.equals(Body)) {
				types.clear();
				types.add(DataType.Model.name());
			}
			else {
				types.remove(DataType.Model.name());
				if (!this.equals(Form)) {
					types.remove(DataType.File.name());
				}
			}
			return types;
		}
	}
	
	public enum DataContent {
		noConvert("Do not convert"),
		useHeader("Use header specific"),
		//toBinary("Binary"),
		toJson("Json"),
		toXml("Xml");
		
		private final String label;
		
		DataContent(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
	}

	public enum DataType {
		Boolean,
		Integer,
		Number,
		String,
		File,
		Model;
	}
	
	public UrlMappingParameter() {
		super();
		setValueOrNull(null);
		
		databaseType = "UrlMappingParameter";
		
		this.priority = getNewOrderValue();
	}
	
	@Override
	public UrlMappingParameter clone() throws CloneNotSupportedException {
		UrlMappingParameter clonedObject = (UrlMappingParameter)super.clone();
		clonedObject.nullProps = nullProps;
		return clonedObject;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		try {
			long priority = Long.valueOf(element.getAttribute("priority")).longValue();
			if (priority == 0L) {
				priority = getNewOrderValue();
				element.setAttribute("priority", ""+priority);
			}
		}
        catch(Exception e) {
            throw new EngineException("Unable to preconfigure the urlmappingparameter \"" + getName() + "\".", e);
        }
	}
	
	abstract public Type getType();
	
	protected Boolean required = Boolean.FALSE;
	
	public Boolean isRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}
	
	private Boolean multiValued = Boolean.FALSE;
	
	public Boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(Boolean multiValued) {
		this.multiValued = multiValued;
	}
	
	private Boolean array = Boolean.FALSE;
	
	public Boolean isArray() {
		return array;
	}

	public void setArray(Boolean array) {
		this.array = array;
	}

	private Boolean exposed = Boolean.TRUE;
	
	public Boolean isExposed() {
		return exposed;
	}

	public void setExposed(Boolean exposed) {
		this.exposed = exposed;
	}

	transient private Set<String> nullProps = new HashSet<String>();
	private Object value = null;
	
	public Object getDefaultValue() {
		return value;
	}

	public void setDefaultValue(Object value) {
		this.value = value;
	}
	
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
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
	
	public Boolean isNullProperty(String propertyName) {
		return nullProps.contains(propertyName);
	}

	public void setNullProperty(String propertyName, Boolean isNull) {
		if (isNull)
			nullProps.add(propertyName);
		else
			nullProps.remove(propertyName);
	}
	
	private String mappedVariableName = "";

	public String getMappedVariableName() {
		return mappedVariableName;
	}

	public void setMappedVariableName(String mappedVariableName) {
		this.mappedVariableName = mappedVariableName;
	}
	
	protected DataContent intputContent = DataContent.useHeader;
	
	public DataContent getInputContent() {
		return intputContent;
	}

	public void setInputContent(DataContent intputContent) {
		this.intputContent = intputContent;
	}

	protected String inputType = DataType.String.name();
	
	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}
	
	public DataType getDataType() {
		return DataType.valueOf(inputType);
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("inputType")) {
			List<String> types = getType().getAllowedDataTypes();
			String[] tags = types.toArray(new String[types.size()]);
			return tags;
		}
		return new String[0];
	}

	protected String getLabel() throws EngineException {
		Object value = getValueOrNull();
		if (value!=null) {
			boolean isString = value instanceof String;
			return " ="+ (isString? "\"":"") + Visibility.Studio.printValue(0,value) + (isString? "\"":"");
		}
		return "";
	}
	
	@Override
	public String toString() {
		String label = "";
		try {
			label = getLabel();
		} catch (EngineException e) {}
		return super.toString() + label;
	}
	
	@Override
	public FolderType getFolderType() {
		return FolderType.MAPPING;
	}
}
