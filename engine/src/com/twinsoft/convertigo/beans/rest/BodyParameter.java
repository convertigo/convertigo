/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.rest;

import com.twinsoft.convertigo.beans.core.IMappingRefModel;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;

public class BodyParameter extends UrlMappingParameter implements IMappingRefModel {

	private static final long serialVersionUID = 4569438940311244772L;

	public BodyParameter() {
		super();
		this.intputContent = DataContent.toJson;
		this.inputType = DataType.Model.name();
	}

	@Override
	public BodyParameter clone() throws CloneNotSupportedException {
		BodyParameter clonedObject = (BodyParameter) super.clone();
		return clonedObject;
	}

	@Override
	public Type getType() {
		return Type.Body;
	}
	
	@Override
	public DataType getDataType() {
		return DataType.Model;
	}

	private String modelReference = "";
	
	public String getModelReference() {
		return modelReference;
	}
	
	public void setModelReference(String modelReference) {
		this.modelReference = modelReference;
	}
}
