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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.twinsoft.convertigo.engine.util.EnumUtils;

public abstract class UrlMappingParameter extends DatabaseObject implements ITagsProperty{

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
		toBinary("Binary"),
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
	}
	
	@Override
	public UrlMappingParameter clone() throws CloneNotSupportedException {
		UrlMappingParameter clonedObject = (UrlMappingParameter)super.clone();
		return clonedObject;
	}

	abstract public Type getType();
	
	private Boolean required = Boolean.TRUE;
	
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

	private String mappedVariableName = "";

	public String getMappedVariableName() {
		return mappedVariableName;
	}

	public void setMappedVariableName(String mappedVariableName) {
		this.mappedVariableName = mappedVariableName;
	}
	
	private DataContent intputContent = DataContent.useHeader;
	
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
	
}
